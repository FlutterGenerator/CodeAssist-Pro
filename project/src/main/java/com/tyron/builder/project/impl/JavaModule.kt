package com.tyron.builder.project.impl

import android.util.Log
import com.itsaky.androidide.utils.DocumentUtils
import com.itsaky.androidide.utils.SourceClassTrie
import com.itsaky.androidide.utils.StopWatch
import com.tyron.builder.project.api.AndroidContentRoot
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

fun JavaModuleImpl.indexSources() {

    this.compileJavaSourceClasses.clear()

    val watch = StopWatch("Indexing sources")
    var count = 0
    val sourceDirs = mutableSetOf<File>()
    if (this is AndroidModuleImpl) {
        contentRoots.forEach {
            val root = it as AndroidContentRoot
            sourceDirs.addAll(root.javaDirectories)
        }
    } else {
        contentRoots.forEach { sourceDirs.addAll(it.sourceDirectories) }
    }
    sourceDirs.forEach {
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

    watch.log()
    Log.d("JavaModuleKt", "Found $count source files.")
}

fun JavaModuleImpl.listClassesFromSourceDirs(packageName: String): List<SourceClassTrie.SourceNode> {
    return compileJavaSourceClasses
        .findInPackage(packageName)
        .filterIsInstance(SourceClassTrie.SourceNode::class.java)
}

fun JavaModuleImpl.packageNameOrEmpty(file: Path?): String {
    if (file == null) {
        return ""
    }

    val sourceNode = searchSourceFileRelatively(file)
    if (sourceNode != null) {
        return sourceNode.packageName
    }

    return ""
}

fun JavaModuleImpl.searchSourceFileRelatively(file: Path?): SourceClassTrie.SourceNode? {
    val sourceDirs = mutableSetOf<File>()
    if (this is AndroidModuleImpl) {
        contentRoots.forEach {
            val root = it as AndroidContentRoot
            sourceDirs.addAll(root.javaDirectories)
        }
    } else {
        contentRoots.forEach { sourceDirs.addAll(it.sourceDirectories) }
    }
    for (source in sourceDirs.map(File::toPath)) {
        val relative = source.relativize(file)
        if (relative.pathString.contains("..")) {
            // This is most probably not the one we're expecting
            continue
        }

        var name = relative.pathString.substringBeforeLast(".java")
        name = name.replace('/', '.')

        val node = this.compileJavaSourceClasses.findNode(name)
        if (node != null && node is SourceClassTrie.SourceNode) {
            return node
        }
    }

    return null
}