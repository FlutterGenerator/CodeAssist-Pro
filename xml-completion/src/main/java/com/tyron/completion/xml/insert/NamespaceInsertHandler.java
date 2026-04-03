package com.tyron.completion.xml.insert;

import com.tyron.completion.model.CompletionItem;
import com.tyron.editor.Caret;
import com.tyron.editor.Editor;

public class NamespaceInsertHandler extends DefaultXmlInsertHandler {

  public NamespaceInsertHandler(CompletionItem item) {
    super(item);
  }

  @Override
  protected void insert(String string, Editor editor, boolean calcSpace) {
    String namespaceValue = String.valueOf(item.data);

    Caret caret = editor.getCaret();
    int startLine = caret.getStartLine();
    int startColumn = caret.getStartColumn();
    String lineString = editor.getContent().getLineString(startLine);
    String substring = lineString.substring(startColumn);

    if (substring.startsWith("=\"\"")) {
      editor.setSelection(startLine, startColumn + 1);
      super.insert(namespaceValue, editor, false);
      editor.setSelection(startLine, startColumn + 1 + namespaceValue.length() + 1);
    } else if (substring.startsWith("=\"")) {
      editor.setSelection(startLine, startColumn + 2);
      super.insert(namespaceValue, editor, false);
      super.insert("\"", editor, false);
      editor.setSelection(caret.getStartLine(), caret.getStartColumn());
    } else {
      super.insert("=\"" + namespaceValue + "\"", editor, false);
      editor.setSelection(startLine, startColumn + namespaceValue.length() + 3);
    }
  }
}
