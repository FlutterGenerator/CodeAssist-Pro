package com.tyron.code.ui.editor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.preference.PreferenceManager
import com.itsaky.androidide.editor.api.IEditor
import com.itsaky.androidide.editor.api.ILspEditor
import com.itsaky.androidide.eventbus.events.editor.ChangeType
import com.itsaky.androidide.eventbus.events.editor.DocumentChangeEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentCloseEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentOpenEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentSaveEvent
import com.itsaky.androidide.eventbus.events.editor.DocumentSelectedEvent
import com.itsaky.androidide.lsp.api.ILanguageClient
import com.itsaky.androidide.lsp.api.ILanguageServer
import com.itsaky.androidide.lsp.api.SignatureHelpLanguage
import com.itsaky.androidide.lsp.models.Command
import com.itsaky.androidide.lsp.models.SignatureHelp
import com.itsaky.androidide.lsp.models.SignatureHelpParams
import com.itsaky.androidide.models.Position
import com.itsaky.androidide.models.Range
import com.itsaky.androidide.progress.ICancelChecker
import com.itsaky.androidide.tasks.JobCancelChecker
import com.itsaky.androidide.tasks.cancelIfActive
import com.tyron.code.ui.editor.snippets.AbstractSnippetVariableResolver
import com.tyron.code.ui.editor.snippets.FileVariableResolver
import com.tyron.code.ui.editor.snippets.WorkspaceVariableResolver
import com.tyron.common.SharedPreferenceKeys
import com.tyron.common.util.CancelChecker
import com.tyron.completion.lsp.api.LspLanguage
import com.tyron.editor.Editor
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File

/**
 *
 * @author Wadamzmail
 */

abstract class IDEEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
    private val editorFeatures: EditorFeatures = EditorFeatures()
) : CodeEditor(context, attrs, defStyleAttr, defStyleRes), IEditor by editorFeatures, Editor,
    ILspEditor {

    protected var _actionsMenu: EditorActionsMenu? = null
    protected var _signatureHelpWindow: SignatureHelpWindow? = null
    protected var _diagnosticWindow: DiagnosticWindow? = null
    private var sigHelpCancelChecker: ICancelChecker? = null

    private var fileVersion = 0
    internal var isModified = false

    @JvmField
    var mCurrentFile: File? = null

    var languageServer: ILanguageServer? = null
        private set

    var languageClient: ILanguageClient? = null
        private set

    /**
     * The [CoroutineScope] for the editor.
     *
     * All the jobs in this scope are cancelled when the editor is released.
     */
    val editorScope = CoroutineScope(Dispatchers.Default + CoroutineName("IDEEditor"))

    protected val eventDispatcher = EditorEventDispatcher()

    private val selectionChangeHandler = Handler(Looper.getMainLooper())
    private var selectionChangeRunner: Runnable? = Runnable {
        val cursor = this.cursor ?: return@Runnable

        if (cursor.isSelected || _signatureHelpWindow?.isShowing == true) {
            return@Runnable
        }

        onSelectionChange()

    }

    protected abstract fun onSelectionChange()

    companion object {
        private const val SELECTION_CHANGE_DELAY = 500L
        internal val log = LoggerFactory.getLogger(IDEEditor::class.java)
    }

    init {
        run {
            editorFeatures.editor = this
            eventDispatcher.editor = this
            eventDispatcher.init(editorScope)
            initEditor()
        }
    }

    val signatureHelpWindow: SignatureHelpWindow
        get() {
            return _signatureHelpWindow ?: SignatureHelpWindow(this).also {
                _signatureHelpWindow = it
            }
        }

    /**
     * The diagnostic window for the editor.
     */
    val diagnosticWindow: DiagnosticWindow
        get() {
            return _diagnosticWindow ?: DiagnosticWindow(this).also { _diagnosticWindow = it }
        }

    fun getCurrentDiagnosticMessage(): String {
        val languageClient = languageClient ?: return ""
        val cursor = this.cursor ?: return ""
        val file = this.file ?: return ""
        return languageClient.getDiagnosticAt(file, cursor.leftLine, cursor.leftColumn)?.message
            ?: ""
    }

    private fun iskotlinAnalysisEnabled(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(SharedPreferenceKeys.KOTLIN_HIGHLIGHTING, false)
    }

    override fun setLanguageServer(server: ILanguageServer?) {
        if (isReleased) {
            return
        }
        this.languageServer = server
        server?.also {
            this.languageClient = it.client
            snippetController.apply {
                fileVariableResolver = FileVariableResolver(this@IDEEditor)
                workspaceVariableResolver = WorkspaceVariableResolver()
            }
        }
    }

    override fun setLanguageClient(client: ILanguageClient?) {
        if (isReleased) {
            return
        }
        this.languageClient = client
    }

    override fun executeCommand(command: Command?) {
        if (isReleased) {
            return
        }
        if (command == null) {
            log.warn("Cannot execute command in editor. Command is null.")
            return
        }

        log.info(String.format("Executing command '%s' for completion item.", command.title))
        when (command.command) {
            Command.TRIGGER_COMPLETION -> {
                val completion = getComponent(EditorAutoCompletion::class.java)
                completion.requireCompletion()
            }

            Command.TRIGGER_PARAMETER_HINTS -> signatureHelp()
            Command.FORMAT_CODE -> formatCodeAsync()
        }
    }

    override fun signatureHelp() {
        if (isReleased) {
            return
        }
        val file = this.file ?: return

        val language = this.editorLanguage as? SignatureHelpLanguage ?: return

        sigHelpCancelChecker?.also { it.cancel() }

        val cancelChecker = JobCancelChecker().also {
            this.sigHelpCancelChecker = it
        }

        editorScope.launch(Dispatchers.Default) {
            cancelChecker.job = coroutineContext[Job]

            val help = safeGet("signature help request") {
                val params = SignatureHelpParams(file.toPath(), cursorLSPPosition, cancelChecker)
                language.signatureHelp(params)
            }

            withContext(Dispatchers.Main) {
                showSignatureHelp(help)
            }
        }.logError("signature help request")
    }

    override fun showSignatureHelp(help: SignatureHelp?) {
        if (isReleased) {
            return
        }
        signatureHelpWindow.setupAndDisplay(help)
    }

    /**
     * If any language server instance is set, finds the references to of the token at the current
     * cursor position.
     *
     * If the server returns a valid response, that response is forwarded to the [ ].
     */
    override fun findReferences() {
//        TODO("Not yet implemented")
    }

    /**
     * Requests the language server to provided a semantically larger selection than the current
     * selection. If a valid response is received, that range will be selected.
     */
    override fun expandSelection() {
//        TODO("Not yet implemented")
    }

    /**
     * If any language server is set, asks the language server to find the definition of token at the
     * cursor position.
     *
     * If the server returns a valid response, and the file specified in the response is same the file
     * in this editor, the range specified in the response will be selected.
     */
    override fun findDefinition() {
//        TODO("Not yet implemented")
    }

    override fun ensureWindowsDismissed() {
        if (_diagnosticWindow?.isShowing == true) {
            _diagnosticWindow?.dismiss()
        }

        if (_signatureHelpWindow?.isShowing == true) {
            _signatureHelpWindow?.dismiss()
        }
//        if (_actionsMenu?.isShowing == true) {
//            _actionsMenu?.dismiss()
//        }
    }

    override fun release() {
        ensureWindowsDismissed()

        if (isReleased) {
            return
        }

        super.release()

        fileVersion = 0

        snippetController.apply {
            (fileVariableResolver as? AbstractSnippetVariableResolver?)?.close()
            (workspaceVariableResolver as? AbstractSnippetVariableResolver?)?.close()

            fileVariableResolver = null
            workspaceVariableResolver = null
        }

//        _actionsMenu?.destroy()
//        _actionsMenu = null
        _signatureHelpWindow = null
        _diagnosticWindow = null
        editorFeatures.editor = null

        languageServer = null
        languageClient = null

        selectionChangeRunner?.also { selectionChangeHandler.removeCallbacks(it) }
        selectionChangeRunner = null

//        if (EventBus.getDefault().isRegistered(this)) {
//            EventBus.getDefault().unregister(this)
//        }

        if (editorScope.isActive) {
            editorScope.cancelIfActive("Editor is releasing resources.")
        }
    }

    /**
     * Initialize the editor.
     */
    protected open fun initEditor() {

//        _actionsMenu = EditorActionsMenu(this).also {
//            it.init()
//        }

        DiagnosticWindow(this).also { _diagnosticWindow = it }
        SignatureHelpWindow(this).also { _signatureHelpWindow = it }

        snippetController.apply {
            fileVariableResolver = FileVariableResolver(this@IDEEditor)
            workspaceVariableResolver = WorkspaceVariableResolver()
        }

//        getComponent(EditorTextActionWindow::class.java).isEnabled = false

        subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
            if (isReleased) {
                return@subscribeEvent
            }
            if (editorLanguage is LspLanguage && file == null) {
                return@subscribeEvent
            }

            editorScope.launch {
                dispatchDocumentChangeEvent(event)
                checkForSignatureHelp(event)
            }
        }

        subscribeEvent(SelectionChangeEvent::class.java) { _, _ ->
            if (isReleased) {
                return@subscribeEvent
            }

            if (_diagnosticWindow?.isShowing == true) {
                _diagnosticWindow?.dismiss()
            }

            selectionChangeRunner?.also {
                selectionChangeHandler.removeCallbacks(it)
                selectionChangeHandler.postDelayed(it, SELECTION_CHANGE_DELAY)
            }
        }
//        EventBus.getDefault().register(this)
    }

    open fun onEditorSelected() {
        if (isReleased) {
            return
        }
        file ?: return
        dispatchDocumentSelectedEvent()
    }

    open fun dispatchDocumentOpenEvent() {
        if (isReleased) {
            return
        }
        if (languageServer == null) {
            return
        }

        val file = this.file ?: return

        this.fileVersion = 0

        val openEvent = DocumentOpenEvent(file.toPath(), text.toString(), fileVersion)

        eventDispatcher.dispatch(openEvent)
    }

    open fun dispatchDocumentChangeEvent(event: ContentChangeEvent) {
        if (isReleased) {
            return
        }
        if (languageServer == null) {
            return
        }

        val file = file?.toPath() ?: return
        var type = ChangeType.INSERT
        if (event.action == ContentChangeEvent.ACTION_DELETE) {
            type = ChangeType.DELETE
        } else if (event.action == ContentChangeEvent.ACTION_SET_NEW_TEXT) {
            type = ChangeType.NEW_TEXT
        }
        var changeDelta = if (type == ChangeType.NEW_TEXT) 0 else event.changedText.length
        if (type == ChangeType.DELETE) {
            changeDelta = -changeDelta
        }
        val start = event.changeStart
        val end = event.changeEnd
        val changeRange = Range(
            Position(start.line, start.column, start.index),
            Position(end.line, end.column, end.index)
        )
        val changedText = event.changedText.toString()
        val changeEvent = DocumentChangeEvent(
            file, changedText, text.toString(), ++fileVersion, type,
            changeDelta, changeRange
        )

        eventDispatcher.dispatch(changeEvent)
    }

    open fun dispatchDocumentSelectedEvent() {
        if (isReleased) {
            return
        }
        if (languageServer == null) {
            return
        }
        val file = file ?: return
        eventDispatcher.dispatch(DocumentSelectedEvent(file.toPath()))
    }

    open fun dispatchDocumentCloseEvent() {
        if (isReleased) {
            return
        }
        if (languageServer == null) {
            return
        }
        val file = file ?: return

        eventDispatcher.dispatch(DocumentCloseEvent(file.toPath(), cursorLSPRange))
    }

    /**
     * Dispatches the [DocumentSaveEvent] for this editor.
     */
    open fun dispatchDocumentSaveEvent() {
        if (isReleased) {
            return
        }
        if (languageServer == null) {
            return
        }
        val file = file ?: return

        eventDispatcher.dispatch(DocumentSaveEvent(file.toPath()))
    }


    /**
     * Checks if the content change event should trigger signature help. Signature help trigger
     * characters are :
     *
     *
     *  * `'('` (parentheses)
     *  * `','` (comma)
     *
     *
     * @param event The content change event.
     */
    private fun checkForSignatureHelp(event: ContentChangeEvent) {
        if (isReleased) {
            return
        }
        val changeLength = event.changedText.length
        if (event.action != ContentChangeEvent.ACTION_INSERT || changeLength < 1 || changeLength > 2) {
            // change length will be 1 if ',' is inserted
            // changeLength will be 2 as '(' and ')' are inserted at the same time
            return
        }

        val ch = event.changedText[0]
        if (ch == '(' || ch == ',') {
            signatureHelp()
        }
    }

    /**
     * Analyze the opened file and publish the diagnostics result.
     */
    open fun analyze() {
        if (isReleased) {
            return
        }
        //if (editorLanguage !is IDELanguage) {
        //   return
        //  }

        val languageServer = languageServer ?: return
        val file = file ?: return

        editorScope.launch {
            val result = safeGet("LSP file analysis") { languageServer.analyze(file.toPath()) }
            languageClient?.publishDiagnostics(result)
        }.logError("LSP file analysis")
    }

    open fun notifyClose() {
        if (isReleased) {
            return
        }
        file ?: run {
            log.info("Cannot notify language server. File is null")
            return
        }
        dispatchDocumentCloseEvent()

//        _actionsMenu?.unsubscribeEvents()
        selectionChangeRunner?.also {
            selectionChangeHandler.removeCallbacks(it)
        }
        selectionChangeRunner = null

        ensureWindowsDismissed()
    }

    private inline fun <T> safeGet(name: String, action: () -> T): T? {
        return try {
            action()
        } catch (err: Throwable) {
            logError(err, name)
            null
        }
    }

    private fun Job.logError(action: String): Job = apply {
        invokeOnCompletion { err -> logError(err, action) }
    }

    private fun logError(err: Throwable?, action: String) {
        err ?: return
        if (CancelChecker.isCancelled(err)) {
            log.warn("{} has been cancelled", action)
        } else {
            log.error("{} failed", action)
        }
    }

    override fun setSelectionAround(line: Int, column: Int) {
        editorFeatures.setSelectionAround(line, column)
    }

} 