package com.tyron.completion.java.rewrite;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
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

public class GenerateEqualsAndHashCode implements JavaRewrite2 {

    private final List<String> fieldNames;
    private final int position;

    public GenerateEqualsAndHashCode(List<String> fieldNames, int position) {
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

        List<TextEdit> edits = new ArrayList<>();
        int indent = EditHelper.indent(context, unit, classTree) + 1;
        String tabs = "\t".repeat(indent);

        // equals()
        MethodDeclaration equalsMethod = createEquals(className, selectedFields);
        String equalsPrinted = "\n" + tabs + JavaParserUtil.prettyPrint(equalsMethod, n -> false).replace("\n", "\n" + tabs) + "\n";

        // hashCode()
        MethodDeclaration hashCodeMethod = createHashCode(selectedFields);
        String hashCodePrinted = "\n" + tabs + JavaParserUtil.prettyPrint(hashCodeMethod, n -> false).replace("\n", "\n" + tabs) + "\n";

        Position insertPos = EditHelper.insertAtEndOfClass(context, unit, classTree);
        insertPos.line -= 1;
        
        TextEdit edit = new TextEdit(new Range(insertPos, insertPos), equalsPrinted + hashCodePrinted);
        java.io.File file = new java.io.File(unit.getSourceFile().toUri());
        
        edits.add(edit);

        // Add java.util.Objects import if needed
        if (!selectedFields.isEmpty()) {
            AddImport addImport = new AddImport(new java.io.File(unit.getSourceFile().toUri()), "java.util.Objects");
            Map<Path, TextEdit[]> importRewrite = addImport.rewrite(context);
            if (importRewrite != null && importRewrite.containsKey(file.toPath())) {
                Collections.addAll(edits, importRewrite.get(file.toPath()));
            }
        }

        return Collections.singletonMap(file.toPath(), edits.toArray(new TextEdit[0]));
    }

    private MethodDeclaration createEquals(String className, List<VariableTree> fields) {
        MethodDeclaration method = new MethodDeclaration();
        method.setModifiers(Keyword.PUBLIC);
        method.addMarkerAnnotation(Override.class);
        method.setType("boolean");
        method.setName("equals");
        method.addParameter(new Parameter(StaticJavaParser.parseType("Object"), "o"));

        BlockStmt body = new BlockStmt();
        
        // if (this == o) return true;
        body.addStatement(new IfStmt(
            new BinaryExpr(new NameExpr("this"), new NameExpr("o"), BinaryExpr.Operator.EQUALS),
            new ReturnStmt(new NameExpr("true")),
            null
        ));

        // if (o == null || getClass() != o.getClass()) return false;
        body.addStatement(new IfStmt(
            new BinaryExpr(
                new BinaryExpr(new NameExpr("o"), new NullLiteralExpr(), BinaryExpr.Operator.EQUALS),
                new BinaryExpr(
                    new MethodCallExpr(null, "getClass"),
                    new MethodCallExpr(new NameExpr("o"), "getClass"),
                    BinaryExpr.Operator.NOT_EQUALS
                ),
                BinaryExpr.Operator.OR
            ),
            new ReturnStmt(new NameExpr("false")),
            null
        ));

        // ClassName that = (ClassName) o;
        body.addStatement(new com.github.javaparser.ast.stmt.ExpressionStmt(
            new com.github.javaparser.ast.expr.VariableDeclarationExpr(
                new com.github.javaparser.ast.body.VariableDeclarator(
                    StaticJavaParser.parseType(className),
                    "that",
                    new CastExpr(StaticJavaParser.parseType(className), new NameExpr("o"))
                )
            )
        ));

        if (fields.isEmpty()) {
            body.addStatement(new ReturnStmt(new NameExpr("true")));
        } else {
            Expression returnExpr = null;
            for (VariableTree field : fields) {
                String name = field.getName().toString();
                MethodCallExpr call = new MethodCallExpr(new NameExpr("java.util.Objects"), "equals");
                call.addArgument(new NameExpr(name));
                call.addArgument(new NameExpr("that." + name));
                
                if (returnExpr == null) {
                    returnExpr = call;
                } else {
                    returnExpr = new BinaryExpr(returnExpr, call, BinaryExpr.Operator.AND);
                }
            }
            body.addStatement(new ReturnStmt(returnExpr));
        }

        method.setBody(body);
        return method;
    }

    private MethodDeclaration createHashCode(List<VariableTree> fields) {
        MethodDeclaration method = new MethodDeclaration();
        method.setModifiers(Keyword.PUBLIC);
        method.addMarkerAnnotation(Override.class);
        method.setType("int");
        method.setName("hashCode");

        BlockStmt body = new BlockStmt();
        MethodCallExpr call = new MethodCallExpr(new NameExpr("java.util.Objects"), "hash");
        for (VariableTree field : fields) {
            call.addArgument(new NameExpr(field.getName().toString()));
        }
        body.addStatement(new ReturnStmt(call));

        method.setBody(body);
        return method;
    }
}
