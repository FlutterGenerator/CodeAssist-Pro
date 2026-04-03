package com.tyron.completion.java.action.generators;

import android.app.Activity;
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.tree.JCTree;
import com.tyron.actions.ActionPlaces;
import com.tyron.actions.AnAction;
import com.tyron.actions.AnActionEvent;
import com.tyron.actions.CommonDataKeys;
import com.tyron.actions.Presentation;
import com.tyron.completion.java.R;
import com.tyron.completion.java.action.FindCurrentPath;
import com.tyron.completion.java.parse.CompilationInfo;
import com.tyron.completion.java.provider.DefaultJavacUtilitiesProvider;
import com.tyron.completion.java.rewrite.GenerateToString;
//import com.tyron.completion.java.util.RewriteUtil;
import com.tyron.completion.java.util.TreeUtil;
import com.tyron.completion.util.RewriteUtil;
import com.tyron.editor.Editor;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;

public class GenerateToStringAction extends AnAction {

    public static final String ID = "javaGenerateToStringAction";

    @Override
    public void update(@NonNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setVisible(false);

        if (!ActionPlaces.EDITOR.equals(event.getPlace())) {
            return;
        }

        File file = event.getData(CommonDataKeys.FILE);
        if (file == null || !file.getName().endsWith(".java")) {
            return;
        }

        presentation.setVisible(true);
        presentation.setText(event.getDataContext().getString(R.string.menu_generators_generate_to_string_title));
    }

    @Override
    public void actionPerformed(@NonNull AnActionEvent e) {
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        File file = e.getRequiredData(CommonDataKeys.FILE);
        CompilationInfo compilationInfo = e.getRequiredData(CompilationInfo.COMPILATION_INFO_KEY);
        
        JCTree.JCCompilationUnit unit = compilationInfo.getCompilationUnit(file.toURI());
        if (unit == null) return;
        
        JavacTaskImpl javacTask = compilationInfo.impl.getJavacTask();
        TreePath currentPath = new FindCurrentPath(javacTask).scan(unit, editor.getCaret().getStart(), editor.getCaret().getEnd());
        TreePath classPath = TreeUtil.findParentOfType(currentPath, ClassTree.class);
        if (classPath == null) return;
        
        ClassTree classTree = (ClassTree) classPath.getLeaf();
        List<String> fieldNames = new ArrayList<>();
        for (Tree member : classTree.getMembers()) {
            if (member instanceof VariableTree) {
                VariableTree vt = (VariableTree) member;
                if (!vt.getModifiers().getFlags().contains(Modifier.STATIC)) {
                    fieldNames.add(vt.getName().toString());
                }
            }
        }

        boolean[] checkedItems = new boolean[fieldNames.size()];
        String[] items = fieldNames.toArray(new String[0]);

        new MaterialAlertDialogBuilder(e.getDataContext())
                .setTitle(R.string.menu_generators_generate_to_string_title)
                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    List<String> selectedFields = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedFields.add(items[i]);
                        }
                    }
                    GenerateToString rewrite = new GenerateToString(selectedFields, editor.getCaret().getStart());
                    RewriteUtil.performRewrite(editor, file, new DefaultJavacUtilitiesProvider(javacTask, unit, editor.getProject()), rewrite);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
