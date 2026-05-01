package com.tyron.common.util

import java.util.concurrent.CancellationException

/** @author Akash Yadav */
class CancelChecker {

  companion object {

    @JvmStatic
    fun isCancelled(err: Throwable?): Boolean {
      if (err == null) {
        return false
      }

      return err is CancellationException ||
          err is CancelAbort ||
          isCancelled(err.cause)
    }
  }
}