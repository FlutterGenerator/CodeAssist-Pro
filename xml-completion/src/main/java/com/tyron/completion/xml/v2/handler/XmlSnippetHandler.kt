package com.tyron.completion.xml.v2.handler

import com.tyron.completion.model.CompletionList
import com.tyron.completion.model.SnippetCompletionItem
import com.tyron.completion.model.snippets.ISnippet
import io.github.rosemoe.sora.lang.completion.SnippetDescription
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser

object XmlSnippetHandler {

    fun addSnippets(
        builder: CompletionList.Builder,
        scope: XmlSnippetScope,
        partial: String
    ) {
        val snippets = XmlSnippetRepository.snippets[scope] ?: return
        val lowercasePartial = partial.lowercase()

        for (snippet in snippets) {
            if (snippet.prefix.lowercase().startsWith(lowercasePartial)) {
                val item = createSnippetItem(snippet, partial.length)
                builder.addItem(item)
            }
        }
    }

    private fun createSnippetItem(snippet: ISnippet, partialLength: Int): SnippetCompletionItem {
        val body = snippet.body.joinToString("\n")
        val codeSnippet = CodeSnippetParser.parse(body)
        val description = SnippetDescription(partialLength, codeSnippet, true)
        return SnippetCompletionItem(snippet.prefix, snippet.description, description)
    }
}
