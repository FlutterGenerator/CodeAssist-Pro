package com.tyron.completion.xml.v2.handler

import com.itsaky.androidide.lsp.snippets.ISnippet
import com.itsaky.androidide.lsp.snippets.SnippetRegistry

object XmlSnippetRepository {

    val snippets: Map<XmlSnippetScope, List<ISnippet>>
        get() = XmlSnippetScope.entries.associateWith { scope ->
            SnippetRegistry.getSnippets("xml",scope.filename)
        }

    fun init() {
        SnippetRegistry.initBuiltIn("xml", XmlSnippetScope.entries)
    }
}
