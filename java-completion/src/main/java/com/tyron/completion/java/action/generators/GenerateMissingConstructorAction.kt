package com.tyron.completion.java.action.generators

import android.app.Activity
import com.tyron.actions.ActionPlaces
import com.tyron.actions.AnAction
import com.tyron.actions.AnActionEvent
import com.tyron.actions.CommonDataKeys
import com.tyron.actions.Presentation
import com.tyron.completion.java.R
import com.tyron.editor.Editor
import com.tyron.code.ui.editor.IDEEditor
import org.slf4j.LoggerFactory
import com.tyron.completion.java.rewrite.GenerateRecordConstructor
import com.tyron.completion.java.util.CodeActionUtils
import com.tyron.completion.java.provider.DefaultJavacUtilitiesProvider
import com.tyron.completion.util.RewriteUtil
import com.tyron.completion.java.parse.CompilationInfo
import com.sun.source.tree.LineMap
import com.sun.source.tree.ClassTree
import com.sun.source.tree.VariableTree
import com.sun.source.util.TreePath
import com.tyron.completion.java.util.TreeUtil
import com.tyron.completion.java.action.FindCurrentPath
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import com.tyron.completion.model.Position
import com.tyron.completion.model.Range
import com.tyron.builder.model.DiagnosticWrapper
import javax.lang.model.element.Modifier

class GenerateMissingConstructorAction : AnAction() {
     
   private val diagnosticCode = "compiler.err.var.not.initialized.in.default.constructor"
     
   companion object {
      const val ID: String = "GenerateMissingConstructorAction"
      private val log = LoggerFactory.getLogger(GenerateMissingConstructorAction::class.java)
   } 
   override fun update(event : AnActionEvent){
     var presentation = event.presentation
     presentation.setVisible(false)  
     if (!ActionPlaces.EDITOR.equals(event.place))return
     
     val editor = event.getData(CommonDataKeys.EDITOR)?: return
     val file = event.getRequiredData(CommonDataKeys.FILE)?: return 
     val diagnostic = event.getData(CommonDataKeys.DIAGNOSTIC)?: return
     if (diagnosticCode != diagnostic.code) return
   
     val compilationInfo = event.getData(CompilationInfo.COMPILATION_INFO_KEY)?: return
     presentation.setVisible(true)
     presentation.setText(event.dataContext.getString(R.string.menu_generators_generate_missing_constructor_title))
   }
   
   override fun actionPerformed(event : AnActionEvent){
     val editor = event.getData(CommonDataKeys.EDITOR) as? IDEEditor ?: return 
     val activity = event.getRequiredData(CommonDataKeys.ACTIVITY)
     val file = event.getRequiredData(CommonDataKeys.FILE)
     val diagnostic = event.getRequiredData(CommonDataKeys.DIAGNOSTIC)
     if (diagnosticCode != diagnostic.code) return
     
     val compilationInfo = event.getData(CompilationInfo.COMPILATION_INFO_KEY)?: return
     val unit = compilationInfo.getCompilationUnit(file.toURI())?: return
     val javacTask = compilationInfo.impl.javacTask
     val task = DefaultJavacUtilitiesProvider(javacTask, unit, editor.project)
     
     val currentPath = FindCurrentPath(javacTask).scan(unit, editor.caret.start.toLong(), editor.caret.end.toLong())
     val classPath = TreeUtil.findParentOfType(currentPath, ClassTree::class.java) ?: return
     val classTree = classPath.leaf as ClassTree
     
     val fieldNames = mutableListOf<String>()
     for (member in classTree.members) {
         if (member is VariableTree) {
             if (!member.modifiers.flags.contains(Modifier.STATIC)) {
                 fieldNames.add(member.name.toString())
             }
         }
     }

     if (fieldNames.isEmpty()) {
         val needsConstructor = CodeActionUtils.findClassNeedingConstructor(task, getDiagnosticRange(diagnostic, unit.lineMap)) ?: return
         val rewrite = GenerateRecordConstructor(needsConstructor)
         RewriteUtil.performRewrite(editor, file, task, rewrite)
         return
     }

     val checkedItems = BooleanArray(fieldNames.size)
     val items = fieldNames.toTypedArray()

     MaterialAlertDialogBuilder(activity)
         .setTitle(R.string.menu_generators_generate_constructor_title)
         .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
             checkedItems[which] = isChecked
         }
         .setPositiveButton(android.R.string.ok) { _, _ ->
             if (activity.isFinishing || activity.isDestroyed) return@setPositiveButton
             val selectedFields = mutableListOf<String>()
             for (i in checkedItems.indices) {
                 if (checkedItems[i]) {
                     selectedFields.add(items[i])
                 }
             }

             val needsConstructor = CodeActionUtils.findClassNeedingConstructor(task, getDiagnosticRange(diagnostic, unit.lineMap)) ?: return@setPositiveButton
             val rewrite = GenerateRecordConstructor(needsConstructor)
             RewriteUtil.performRewrite(editor, file, task, rewrite)
         }
         .setNegativeButton(android.R.string.cancel, null)
         .show()
   }
   
   private fun getDiagnosticRange(
    diagnostic: DiagnosticWrapper ,
    lines: LineMap
  ): Range {
    val start = getPosition(diagnostic.startPosition, lines)
    val end = getPosition(diagnostic.endPosition, lines)
    return Range(start, end)
  }

  private fun getPosition(position: Long, lines: LineMap): Position {
    val line = (lines.getLineNumber(position) - 1).toInt()
    val column = (lines.getColumnNumber(position) - 1).toInt()
    return Position(line, column)
  }

}
