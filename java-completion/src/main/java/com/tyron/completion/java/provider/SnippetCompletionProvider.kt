package com.tyron.completion.java.provider

import com.tyron.completion.java.compiler.JavaCompilerService
import com.tyron.completion.java.provider.snippet.JavaSnippetRepository
import com.tyron.completion.java.provider.snippet.JavaSnippetScope
import com.tyron.completion.model.snippets.ISnippet
import com.tyron.completion.model.snippets.DefaultSnippet
import com.tyron.completion.model.SnippetCompletionItem
import io.github.rosemoe.sora.lang.completion.SnippetDescription
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser
import com.sun.source.tree.ClassTree
import com.sun.source.tree.CompilationUnitTree
import com.sun.source.tree.MethodTree
import com.sun.source.util.TreePath
import com.tyron.completion.model.CompletionList

/**
 * Provides snippet completion for Java files.
 *
 * @author Akash Yadav
 * @modified by Wadamzmail
 */
class SnippetCompletionProvider(
  compiler: JavaCompilerService? 
) : BaseCompletionProvider(compiler) {

  companion object {
    private val MANUAL_SNIPPETS = mapOf(
        JavaSnippetScope.GLOBAL to listOf(
            DefaultSnippet("lcomm", "Create a line comment", arrayOf("// \$0")),
            DefaultSnippet("bcomm", "Create a block comment", arrayOf("/*", " * \$0", " */")),
            DefaultSnippet("todo", "Create a TODO comment", arrayOf("// TODO: \$0")),
            DefaultSnippet("fixme", "Create a FIXME comment", arrayOf("// FIXME: \$0")),
            DefaultSnippet("jdoc", "Create a JavaDoc comment", arrayOf("/**", " * \$0", " */"))
        ),
        JavaSnippetScope.LOCAL to listOf(
            DefaultSnippet("sout", "System.out.println()", arrayOf("System.out.println(\$0);")),
            DefaultSnippet("serr", "System.err.println()", arrayOf("System.err.println(\$0);")),
            DefaultSnippet("if", "if statement", arrayOf("if (\${1:condition}) {", "\t\$0", "}")),
            DefaultSnippet("ifelse", "if-else statement", arrayOf("if (\${1:condition}) {", "\t\$0", "} else {", "\t", "}")),
            DefaultSnippet("ifnull", "If-null", arrayOf("if (\${1:object} == null) {", "\t\$0", "}")),
            DefaultSnippet("ifnotnull", "If-not-null", arrayOf("if (\${1:object} != null) {", "\t\$0", "}")),
            DefaultSnippet("for", "Indexed for loop", arrayOf("for (int \${1:i} = 0; \$1 < \${2:count}; ++\$1) {", "\t\$0", "}")),
            DefaultSnippet("foreach", "For-each loop", arrayOf("for (\${1:element} : \${2:iterable}) {", "\t\$0", "}")),
            DefaultSnippet("forr", "Reverse-indexed for loop", arrayOf("for (int \${1:i} = \${2:count} - 1; \$1 >= 0; --\$1) {", "\t\$0", "}")),
            DefaultSnippet("while", "While loop", arrayOf("while (\${1:condition}) {", "\t\$0", "}")),
            DefaultSnippet("dowhile", "Do-While loop", arrayOf("do {", "\t\$0", "} while (\${1:condition});")),
            DefaultSnippet("new_object", "Create a new object", arrayOf("\${1:Object} \${2:foo} = new \$1(\$3);", "\$0")),
            DefaultSnippet("trycatch", "A try-catch statement", arrayOf("try {", "\t\$0", "} catch (\${1:Exception} \${2:err}) {", "\t", "}")),
            DefaultSnippet("tryresources", "A try-with-resources statement", arrayOf("try (\$1) {", "\t\$0", "} catch (\${2:Exception} \${3:err}) {", "\t", "}"))
        ),
        JavaSnippetScope.MEMBER to listOf(
            DefaultSnippet("const", "Create a constant field", arrayOf("public static final \${1:type} \${2:NAME} = \${3:value};")),
            DefaultSnippet("ctor", "Public constructor", arrayOf("public \${1:ClassName}(\$2) {", "\t\${3:super();}\$0", "}")),
            DefaultSnippet("public_method", "Create a new public method", arrayOf("public \${1:void} \${2:method}(\$3) {", "\t\$0", "}")),
            DefaultSnippet("protected_method", "Create a new protected method", arrayOf("protected \${1:void} \${2:method}(\$3) {", "\t\$0", "}")),
            DefaultSnippet("private_method", "Create a new private method", arrayOf("private \${1:void} \${2:method}(\$3) {", "\t\$0", "}")),
            DefaultSnippet("public_static_method", "Create a new public static method", arrayOf("public static \${1:void} \${2:method}(\$3) {", "\t\$0", "}")),
            DefaultSnippet("private_static_method", "Create a new private static method", arrayOf("private static \${1:void} \${2:method}(\$3) {", "\t\$0", "}"))
        ),
        JavaSnippetScope.TOP_LEVEL to listOf(
            DefaultSnippet("class", "Create a new class", arrayOf("public class \${1:ClassName} {", "\t\$0", "}")),
            DefaultSnippet("interface", "Create a new interface", arrayOf("public interface \${1:InterfaceName} {", "\t\$0", "}")),
            DefaultSnippet("enum", "Create a new enum class", arrayOf("public enum \${1:EnumName} {", "\t\$0", "}"))
        )
    )
  }

  override fun complete(
    builder: CompletionList.Builder,
    task: JavacUtilitiesProvider,
    path: TreePath,
    partial: String,
    endsWithParen: Boolean
  ) {
    // 1. Determine current scope
    val scopePath = findSnippetScope(path)
    val currentScope = when (scopePath?.leaf) {
        is CompilationUnitTree -> JavaSnippetScope.TOP_LEVEL
        is ClassTree -> JavaSnippetScope.MEMBER
        is MethodTree -> JavaSnippetScope.LOCAL
        else -> if (isInsideMethod(scopePath)) JavaSnippetScope.LOCAL else null
    }

    // 2. Define scope priority for sorting
    // Lower number = higher priority
    val scopePriority = mapOf(
        JavaSnippetScope.LOCAL to if (currentScope == JavaSnippetScope.LOCAL) 0 else 3,
        JavaSnippetScope.GLOBAL to 1,
        JavaSnippetScope.MEMBER to if (currentScope == JavaSnippetScope.MEMBER) 0 else 2,
        JavaSnippetScope.TOP_LEVEL to if (currentScope == JavaSnippetScope.TOP_LEVEL) 0 else 4
    )

    // 3. Collect and sort all snippets
    val lowercasePartial = partial.lowercase()
    val addedPrefixes = mutableSetOf<String>()

    for (scope in JavaSnippetScope.values()) {
        val priority = scopePriority[scope] ?: 5
        val scopeSnippets = mutableListOf<ISnippet>()
        
        // Add manual snippets
        MANUAL_SNIPPETS[scope]?.let { scopeSnippets.addAll(it) }
        
        // Add repository snippets
        try {
            JavaSnippetRepository.snippets[scope]?.let { scopeSnippets.addAll(it) }
        } catch (ignored: Exception) {}

        for (snippet in scopeSnippets) {
            if (snippet.prefix.lowercase().startsWith(lowercasePartial) && addedPrefixes.add(snippet.prefix)) {
                val item = createSnippetItem(snippet, partial.length)
                
                // Sort by: priority group -> alphabetical prefix
                item.setSortText("%03d_%s".format(priority, snippet.prefix))
                builder.addItem(item)
            }
        }
    }
  }

  /**
   * Creates a [SnippetCompletionItem] that correctly deletes only the [partialLength].
   */
  private fun createSnippetItem(snippet: ISnippet, partialLength: Int): SnippetCompletionItem {
    val body = snippet.body.joinToString("\n")
    val codeSnippet = CodeSnippetParser.parse(body)
    
    // CRITICAL: Use partialLength so it only deletes what the user typed, preserving indentation
    val description = SnippetDescription(partialLength, codeSnippet, true)
    
    return SnippetCompletionItem(snippet.prefix, snippet.description, description)
  }

  private fun isInsideMethod(path: TreePath?): Boolean {
    var p = path
    while (p != null) {
      if (p.leaf is MethodTree) return true
      p = p.parentPath
    }
    return false
  }

  private fun findSnippetScope(path: TreePath?): TreePath? {
    var scope = path
    while (scope != null) {
      val leaf = scope.leaf
      if (leaf is CompilationUnitTree || leaf is ClassTree || leaf is MethodTree) {
        return scope
      }
      scope = scope.parentPath
    }
    return path
  }
}
