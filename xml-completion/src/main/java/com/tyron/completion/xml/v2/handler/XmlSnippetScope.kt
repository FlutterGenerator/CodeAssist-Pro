package com.tyron.completion.xml.v2.handler

import com.itsaky.androidide.lsp.snippets.ISnippetScope

enum class XmlSnippetScope(override val filename: String) : ISnippetScope {
    TAG("tag"),
    ATTRIBUTE("attribute"),
    VALUE("value")
}
