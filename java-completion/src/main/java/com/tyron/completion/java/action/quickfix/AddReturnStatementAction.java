package com.tyron.completion.java.action.quickfix;

import androidx.annotation.NonNull;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.tree.JCTree;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.Presentation;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.completion.java.R;
import com.tyron.completion.java.action.FindCurrentPath;
import com.tyron.completion.java.parse.CompilationInfo;
import com.tyron.completion.java.provider.DefaultJavacUtilitiesProvider;
import com.tyron.completion.java.rewrite.AddReturnStatement;
import com.tyron.completion.java.rewrite.JavaRewrite2;
import com.tyron.completion.java.util.ErrorCodes;
import com.tyron.completion.util.RewriteUtil;
import com.tyron.editor.Editor;
import java.io.File;

public class AddReturnStatementAction extends AnAction {

    public static final String ID = "javaAddReturnStatementFix";

    @Override
    public void update(@NonNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setVisible(false);

        if (!ActionPlaces.EDITOR.equals(event.getPlace())) {
            return;
        }

        DiagnosticWrapper diagnostic = event.getData(CommonDataKeys.DIAGNOSTIC);
        if (diagnostic == null) {
            return;
        }

        if (!ErrorCodes.MISSING_RETURN_STATEMENT.equals(diagnostic.getCode())) {
            return;
        }

        presentation.setText(event.getDataContext().getString(R.string.menu_quickfix_add_return_statement_title));
        presentation.setVisible(true);
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        File file = e.getRequiredData(CommonDataKeys.FILE);
        DiagnosticWrapper diagnostic = e.getRequiredData(CommonDataKeys.DIAGNOSTIC);
        
        CompilationInfo compilationInfo = e.getData(CompilationInfo.COMPILATION_INFO_KEY);
        if (compilationInfo == null) return;
        
        JCTree.JCCompilationUnit unit = compilationInfo.getCompilationUnit(file.toURI());
        if (unit == null) return;
        
        JavacTaskImpl javacTask = compilationInfo.impl.getJavacTask();
        
        JavaRewrite2 rewrite = new AddReturnStatement((int) diagnostic.getStartPosition());
        RewriteUtil.performRewrite(
            editor,
            file,
            new DefaultJavacUtilitiesProvider(javacTask, unit, editor.getProject()),
            rewrite);
    }
}
