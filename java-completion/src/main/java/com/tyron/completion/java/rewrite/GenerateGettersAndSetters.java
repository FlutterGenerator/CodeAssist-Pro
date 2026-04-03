package com.tyron.completion.java.rewrite;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.Modifier.Keyword;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.tyron.completion.java.provider.JavacUtilitiesProvider;
import com.tyron.completion.java.util.JavaParserTypesUtil;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public class GenerateGettersAndSetters implements JavaRewrite2 {

    private final List<String> fieldNames;
    private final int position;

    public GenerateGettersAndSetters(List<String> fieldNames, int position) {
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
        for (Object member : classTree.getMembers()) {
            if (member instanceof VariableTree) {
                VariableTree vt = (VariableTree) member;
                if (fieldNames.contains(vt.getName().toString())) {
                    fields.add(vt);
                }
            }
        }

        if (fields.isEmpty()) {
            return Collections.emptyMap();
        }

        StringBuilder sb = new StringBuilder();
        int indent = EditHelper.indent(task, task.root(), classTree) + 1;
        String tabs = "\t".repeat(indent);

        for (VariableTree field : fields) {
            VariableElement element = (VariableElement) task.getTrees().getElement(new TreePath(path, field));
            TypeMirror type = element.asType();
            String name = field.getName().toString();
            String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);
            
            com.github.javaparser.ast.type.Type jpType = JavaParserTypesUtil.toType(type);

            // Getter
            MethodDeclaration getter = new MethodDeclaration();
            getter.setModifiers(Keyword.PUBLIC);
            getter.setType(jpType);
            getter.setName("get" + capitalizedName);
            BlockStmt getterBody = new BlockStmt();
            getterBody.addStatement(new ReturnStmt(new NameExpr(name)));
            getter.setBody(getterBody);
            
            sb.append("\n").append(tabs).append(JavaParserUtil.prettyPrint(getter, n -> false).replace("\n", "\n" + tabs)).append("\n");

            // Setter
            MethodDeclaration setter = new MethodDeclaration();
            setter.setModifiers(Keyword.PUBLIC);
            setter.setType("void");
            setter.setName("set" + capitalizedName);
            setter.addParameter(new Parameter(jpType, name));
            BlockStmt setterBody = new BlockStmt();
            setterBody.addStatement(new AssignExpr(
                new FieldAccessExpr(new ThisExpr(), name),
                new NameExpr(name),
                AssignExpr.Operator.ASSIGN
            ));
            setter.setBody(setterBody);

            sb.append("\n").append(tabs).append(JavaParserUtil.prettyPrint(setter, n -> false).replace("\n", "\n" + tabs)).append("\n");
        }

        Position insertPos = EditHelper.insertAtEndOfClass(task, task.root(), classTree);
        insertPos.line -= 1; // Before closing brace
        
        TextEdit edit = new TextEdit(new Range(insertPos, insertPos), sb.toString());
        
        java.io.File file = new java.io.File(task.root().getSourceFile().toUri());
        return Collections.singletonMap(file.toPath(), new TextEdit[]{edit});
    }
}
