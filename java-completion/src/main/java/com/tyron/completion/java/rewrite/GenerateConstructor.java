package com.tyron.completion.java.rewrite;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.tyron.completion.java.provider.JavacUtilitiesProvider;
import com.tyron.completion.java.util.JavaParserTypesUtil;
import com.tyron.completion.java.util.JavaParserUtil;
import com.tyron.completion.java.util.TreeUtil;
import com.tyron.completion.model.Position;
import com.tyron.completion.model.Range;
import com.tyron.completion.model.TextEdit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.Modifier.Keyword;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public class GenerateConstructor implements JavaRewrite2 {

    private final List<String> fieldNames;
    private final int position;

    public GenerateConstructor(List<String> fieldNames, int position) {
        this.fieldNames = fieldNames;
        this.position = position;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(JavacUtilitiesProvider task) {
        TreePath path = TreeUtil.findParentOfType(new com.tyron.completion.java.action.FindCurrentPath(task.getTask()).scan(task.root(), position), ClassTree.class);
        if (path == null) {
            return Collections.emptyMap();
        }
        ClassTree classTree = (ClassTree) path.getLeaf();
        
        List<VariableTree> fields = new ArrayList<>();
        for (Tree member : classTree.getMembers()) {
            if (member instanceof VariableTree) {
                VariableTree vt = (VariableTree) member;
                if (fieldNames.contains(vt.getName().toString())) {
                    fields.add(vt);
                }
            }
        }

        String className = classTree.getSimpleName().toString();
        ConstructorDeclaration constructor = new ConstructorDeclaration();
        constructor.setModifiers(Keyword.PUBLIC);
        constructor.setName(className);
        
        BlockStmt body = new BlockStmt();
        for (VariableTree field : fields) {
            VariableElement element = (VariableElement) task.getTrees().getElement(new TreePath(path, field));
            TypeMirror type = element.asType();
            String name = field.getName().toString();
            
            constructor.addParameter(new Parameter(JavaParserTypesUtil.toType(type), name));
            body.addStatement(new AssignExpr(
                new FieldAccessExpr(new ThisExpr(), name),
                new NameExpr(name),
                AssignExpr.Operator.ASSIGN
            ));
        }
        constructor.setBody(body);

        int indent = EditHelper.indent(task, task.root(), classTree) + 1;
        String tabs = "\t".repeat(indent);
        String printed = "\n" + tabs + JavaParserUtil.prettyPrint(constructor, n -> false).replace("\n", "\n" + tabs) + "\n";

        Position insertPos = EditHelper.insertAtEndOfClass(task, task.root(), classTree);
        insertPos.line -= 1; // Before closing brace
        
        TextEdit edit = new TextEdit(new Range(insertPos, insertPos), printed);
        
        java.io.File file = new java.io.File(task.root().getSourceFile().toUri());
        return Collections.singletonMap(file.toPath(), new TextEdit[]{edit});
    }
}
