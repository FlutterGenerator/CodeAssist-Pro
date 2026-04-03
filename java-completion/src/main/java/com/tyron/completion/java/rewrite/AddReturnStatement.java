package com.tyron.completion.java.rewrite;

import com.github.javaparser.ast.stmt.ReturnStmt;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.tyron.completion.java.action.FindCurrentPath;
import com.tyron.completion.java.provider.JavacUtilitiesProvider;
import com.tyron.completion.java.util.JavaParserTypesUtil;
import com.tyron.completion.java.util.TreeUtil;
import com.tyron.completion.model.Position;
import com.tyron.completion.model.Range;
import com.tyron.completion.model.TextEdit;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class AddReturnStatement implements JavaRewrite2 {

    private final int position;

    public AddReturnStatement(int position) {
        this.position = position;
    }

    @Override
    public Map<Path, TextEdit[]> rewrite(JavacUtilitiesProvider task) {
        TreePath path = new FindCurrentPath(task.getTask()).scan(task.root(), position);
        if (path == null) {
            return Collections.emptyMap();
        }
        TreePath methodPath = TreeUtil.findParentOfType(path, MethodTree.class);
        if (methodPath == null) {
            return Collections.emptyMap();
        }
        MethodTree method = (MethodTree) methodPath.getLeaf();

        JCTree.JCBlock body = (JCTree.JCBlock) method.getBody();
        if (body == null) {
            return Collections.emptyMap();
        }

        TypeMirror returnType = ((JCTree.JCMethodDecl) method).restype.type;
        if (returnType == null || returnType.getKind() == TypeKind.VOID) {
            return Collections.emptyMap();
        }

        String returnText = "return " + getDefaultValue(returnType) + ";";
        
        long endPos = task.getTrees().getSourcePositions().getEndPosition(task.root(), body);
        int line = (int) task.root().getLineMap().getLineNumber(endPos);
        int column = (int) task.root().getLineMap().getColumnNumber(endPos);
        
        // Insert before the closing brace
        Position pos = new Position(line - 1, column - 2);
        
        int indent = EditHelper.indent(task, task.root(), body);
        String indentStr = "\n" + "\t".repeat(indent + 1);
        
        TextEdit edit = new TextEdit(new Range(pos, pos), indentStr + returnText);
        
        java.io.File file = new java.io.File(task.root().getSourceFile().toUri());
        Path filePath = file.toPath();
        return Collections.singletonMap(filePath, new TextEdit[]{edit});
    }

    private String getDefaultValue(TypeMirror type) {
        switch (type.getKind()) {
            case BOOLEAN:
                return "false";
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case CHAR:
            case FLOAT:
            case DOUBLE:
                return "0";
            default:
                return "null";
        }
    }
}
