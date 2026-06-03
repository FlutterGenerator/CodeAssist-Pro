package com.tyron.builder.project.impl

import android.util.Log
import com.itsaky.androidide.utils.DocumentUtils
import com.itsaky.androidide.utils.StopWatch
import com.tyron.builder.project.api.AndroidContentRoot

fun AndroidModuleImpl.indexSources() {

    this.compileJavaSourceClasses.clear()

    val watch = StopWatch("Indexing sources")
    var count = 0
    contentRoots.forEach {
        val androidRoot = it as AndroidContentRoot
        androidRoot.javaDirectories.forEach {
            val sourceDir = it.toPath()
            it
                .walk()
                .filter { file -> file.isFile && file.exists() && DocumentUtils.isJavaFile(file.toPath()) }
                .map { file -> file.toPath() }
                .forEach { file ->
                    this.compileJavaSourceClasses.append(file, sourceDir)
                    count++
                }
        }
    }

    watch.log()
    Log.d("AndroidModuleKt","Found $count source files.")
}