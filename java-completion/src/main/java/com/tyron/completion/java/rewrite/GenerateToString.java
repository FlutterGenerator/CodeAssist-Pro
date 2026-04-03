package com.tyron.completion.java.rewrite;

import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.tyron.completion.java.action.FindCurrentPath;
import com.tyron.completion.java.provider.JavacUtilitiesProvider;
import com.tyron.completion.java.util.JavaParserUtil;
import com.tyron.completion.java.util.TreeUtil;
import com.tyron.completion.model.Position;
import com.tyron.completion.model.Range;
import com.tyron.completion.model.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenerateToString implements JavaRewrite2 {

    private final List<String> fieldNames;
    private final int position;

    public GenerateToString(List<String> fieldNames, int position) {
        this.fieldNames = fieldNames;
        this.position = position;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(JavacUtilitiesProvider context) {
        CompilationUnitTree unit = context.root();
        TreePath path = TreeUtil.findParentOfType(new FindCurrentPath(context.getTask()).scan(unit, (long) position), ClassTree.class);
        if (path == null) {
            return Collections.emptyMap();
        }
        ClassTree classTree = (ClassTree) path.getLeaf();
        
        String className = classTree.getSimpleName().toString();
        
        List<? extends Tree> members = classTree.getMembers();
        List<VariableTree> selectedFields = members.stream()
                .filter(m -> m instanceof VariableTree)
                .map(m -> (VariableTree) m)
                .filter(v -> fieldNames.contains(v.getName().toString()))
                .collect(Collectors.toList());

        MethodDeclaration method = new MethodDeclaration();
        method.setModifiers(Keyword.PUBLIC);
        method.addMarkerAnnotation(Override.class);
        method.setType(String.class);
        method.setName("toString");

        BlockStmt body = new BlockStmt();
        
        BinaryExpr returnExpr = new BinaryExpr(
            new StringLiteralExpr(className + "{"),
            new StringLiteralExpr(""),
            BinaryExpr.Operator.PLUS
        );

        for (int i = 0; i < selectedFields.size(); i++) {
            VariableTree field = selectedFields.get(i);
            String name = field.getName().toString();
            String prefix = (i == 0 ? "" : ", ") + name + "=";
            
            returnExpr = new BinaryExpr(
                returnExpr,
                new StringLiteralExpr(prefix),
                BinaryExpr.Operator.PLUS
            );
            returnExpr = new BinaryExpr(
                returnExpr,
                new NameExpr(name),
                BinaryExpr.Operator.PLUS
            );
        }
        
        returnExpr = new BinaryExpr(
            returnExpr,
            new StringLiteralExpr("}"),
            BinaryExpr.Operator.PLUS
        );

        body.addStatement(new ReturnStmt(returnExpr));
        method.setBody(body);

        int indent = EditHelper.indent(context, unit, classTree) + 1;
        String tabs = "\t".repeat(indent);
        String printed = "\n" + tabs + JavaParserUtil.prettyPrint(method, n -> false).replace("\n", "\n" + tabs) + "\n";

        Position insertPos = EditHelper.insertAtEndOfClass(context, unit, classTree);
        insertPos.line -= 1;
        
        TextEdit edit = new TextEdit(new Range(insertPos, insertPos), printed);
        java.io.File file = new java.io.File(unit.getSourceFile().toUri());
        return Collections.singletonMap(file.toPath(), new TextEdit[]{edit});
    }
}
