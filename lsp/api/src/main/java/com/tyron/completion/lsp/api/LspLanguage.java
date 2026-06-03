package com.tyron.completion.lsp.api;

import com.itsaky.androidide.lsp.api.ILanguageServer;

public interface LspLanguage {

  void setLanguageServer(ILanguageServer server);

  ILanguageServer getLanguageServer();
}
