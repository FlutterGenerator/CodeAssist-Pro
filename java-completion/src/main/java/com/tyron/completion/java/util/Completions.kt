package com.tyron.completion.java.util

import com.itsaky.androidide.lsp.snippets.ISnippet
import com.tyron.common.util.indentationString
import com.tyron.completion.model.SnippetCompletionItem
import io.github.rosemoe.sora.lang.completion.SnippetDescription
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser
import java.lang.String

fun snippetItem(snippet: ISnippet,indent: Int): SnippetCompletionItem {
    //val body = String.join("\n", *snippet.body)
    val indentation = indentationString(indent)
   val body = snippet.body.joinToString(separator = "\n").also {
        it.replace("\t", indentationString).replace("\n", "\n${indentation}")
    }
    val cs = CodeSnippetParser.parse(body)

    val item =
        SnippetCompletionItem(
            snippet.prefix,
            snippet.description,
            SnippetDescription(snippet.prefix.length, cs, true)
        )
    item.setSortText(snippet.prefix)
    return item
}