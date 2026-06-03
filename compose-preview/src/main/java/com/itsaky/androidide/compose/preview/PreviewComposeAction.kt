package com.itsaky.androidide.compose.preview

import android.app.Activity
import com.tyron.actions.ActionPlaces
import com.tyron.actions.AnAction
import com.tyron.actions.AnActionEvent
import com.tyron.actions.CommonDataKeys
import java.io.File

class PreviewComposeAction() : AnAction() {

    companion object {
        const val ID: String = "composePreviewAction"
    }

    override fun update(event: AnActionEvent) {
        var presentation = event.presentation
        presentation.setVisible(false)

        if (!ActionPlaces.EDITOR.equals(event.place)) return

        val activity = event.getData(CommonDataKeys.ACTIVITY) ?: return

        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = editor.currentFile ?: return

        if (file.name.endsWith(".kt").not()) return

        val content = editor.content.toString()
        val hasCompose = content.contains("@Composable") ||
                content.contains("androidx.compose") ||
                content.contains("@Preview")
        if (hasCompose.not()) return

        presentation.setVisible(true)
        presentation.setText("Preview Compose")
    }

    /**
     * Implement this method to handle when this action has been clicked or pressed.
     *
     * @param e Carries information on the invocation place and data available.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val activity = event.getData(CommonDataKeys.ACTIVITY) ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val file = editor.currentFile ?: return

        if (file.name.endsWith(".kt").not()) return

        val content = editor.content.toString()
        val hasCompose = content.contains("@Composable") ||
                content.contains("androidx.compose") ||
                content.contains("@Preview")
        if (hasCompose.not()) return
        activity.showComposePreviewSheet(file, editor.content.toString())
    }

    private fun Activity.showComposePreviewSheet(file: File, sourceCode: String) {
        ComposePreviewActivity.start(this, sourceCode, file.absolutePath)
    }
}