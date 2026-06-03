package com.itsaky.androidide.lsp.editor.language

import com.itsaky.androidide.progress.ICancelChecker
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionPublisher

/**
 * [io.github.rosemoe.sora.lang.completion.CompletionPublisher] implementation which exposes the `checkCancelled` method.
 *
 * @author Akash Yadav
 */
class CompletionCancelChecker(private val publisher: CompletionPublisher) :
  ICancelChecker.Default() {

  /** Check if the completion is cancelled. */
  @Throws(CompletionCancelledException::class)
  override fun abortIfCancelled() {
    publisher.checkCancelled()
    super.abortIfCancelled()
  }
}