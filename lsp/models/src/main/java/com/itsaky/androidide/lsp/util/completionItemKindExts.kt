package com.itsaky.androidide.lsp.util

import com.itsaky.androidide.lsp.models.CompletionItemKind
import com.itsaky.androidide.lsp.models.DrawableKind

fun CompletionItemKind.toDrawableKind(): DrawableKind {
    return when (this) {
        CompletionItemKind.KEYWORD -> DrawableKind.Keyword
        CompletionItemKind.VARIABLE -> DrawableKind.LocalVariable
        CompletionItemKind.FIELD -> DrawableKind.Field
        CompletionItemKind.METHOD -> DrawableKind.Method
        CompletionItemKind.FUNCTION -> DrawableKind.Method
        CompletionItemKind.TYPE_PARAMETER -> DrawableKind.Class
        CompletionItemKind.CLASS -> DrawableKind.Class
        CompletionItemKind.INTERFACE -> DrawableKind.Interface
        CompletionItemKind.ENUM -> DrawableKind.Class
        CompletionItemKind.ANNOTATION_TYPE -> DrawableKind.Attribute
        CompletionItemKind.MODULE -> DrawableKind.Package
        CompletionItemKind.SNIPPET -> DrawableKind.Snippet
        CompletionItemKind.VALUE -> DrawableKind.LocalVariable
        CompletionItemKind.NONE -> DrawableKind.LocalVariable
        CompletionItemKind.ENUM_MEMBER -> DrawableKind.EnumMember
        CompletionItemKind.CONSTRUCTOR -> DrawableKind.Constructor
        CompletionItemKind.PROPERTY -> DrawableKind.Property
    }
}