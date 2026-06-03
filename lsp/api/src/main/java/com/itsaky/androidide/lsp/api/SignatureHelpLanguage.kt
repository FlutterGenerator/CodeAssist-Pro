package com.itsaky.androidide.lsp.api

import com.itsaky.androidide.lsp.models.SignatureHelp
import com.itsaky.androidide.lsp.models.SignatureHelpParams
import kotlin.collections.emptyList

interface SignatureHelpLanguage {

    fun signatureHelp(params: SignatureHelpParams): SignatureHelp =
        unsupportedSignatureHelp()
}

fun unsupportedSignatureHelp(): SignatureHelp =
    SignatureHelp(emptyList(), -1, -1)