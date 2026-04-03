package com.tyron.completion.xml.v2.handler

import com.tyron.completion.model.snippets.ISnippet
import com.tyron.completion.model.snippets.SnippetParser

object XmlSnippetRepository {

    lateinit var snippets: Map<XmlSnippetScope, List<ISnippet>>
        private set

    fun init() {
        this.snippets = SnippetParser.parse("xml", XmlSnippetScope.values())
    }
}
