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

fun JavaModuleImpl.getIntermediateClasspaths(): Set<File> {
    val result = mutableSetOf<File>()
//    val variant = getSelectedVariant()?.name ?: "debug"

    val kotlinClasses = File(buildDirectory, "bin/kotlin")
    if (kotlinClasses.exists()) {
        result.add(kotlinClasses)
    }

    val javaClassesDir = File(buildDirectory, "bin/java")
    if (javaClassesDir.exists()) {
        javaClassesDir.walkTopDown()
            .filter { it.name == "classes" && it.isDirectory }
            .forEach { result.add(it) }
    }

//    val rClassDir = File(
//        buildDirectory,
//        "intermediates/compile_and_runtime_not_namespaced_r_class_jar/$variant"
//    )
//    if (rClassDir.exists()) {
//        rClassDir.walkTopDown()
//            .filter { it.name == "R.jar" && it.isFile }
//            .forEach { result.add(it) }
//    }

    return result
}

fun JavaModuleImpl.getRuntimeDexFiles(): Set<File> {
    val result = mutableSetOf<File>()
//    val variant = getSelectedVariant()?.name ?: "debug"

//    log.info(
//        "getRuntimeDexFiles: buildDir={}, variant={}",
//        buildDirectory.absolutePath,
//        variant
//    )

    val dexDir = File(buildDirectory, "libraries")
    Log.i("",String.format("  Checking dexDir: {} (exists: {})", dexDir.absolutePath, dexDir.exists()))
    if (dexDir.exists()) {
        dexDir.walkTopDown()
            .filter { it.name.endsWith(".dex") && it.isFile }
            .forEach {
                Log.i("",String.format("    Found DEX: {}", it.absolutePath))
                result.add(it)
            }
    }
    val lonDex = File(buildDirectory,"bin/classes.dex")
    if (lonDex.exists()){
        result.add(lonDex)
    }

    val mergeProjectDexDir = File(buildDirectory, "intermediates/classes")
    Log.i("",String.format(
        "  Checking project_dex_archive: {} (exists: {})",
        mergeProjectDexDir.absolutePath,
        mergeProjectDexDir.exists()
    ))
    if (mergeProjectDexDir.exists()) {
        mergeProjectDexDir.walkTopDown()
            .filter { it.name.endsWith(".dex") && it.isFile }
            .forEach {
                Log.i("",String.format("    Found DEX: {}",it.absolutePath))
                result.add(it)
            }
    }

    Log.i("",String.format("  Total DEX files found: {}", result.size))
    return result
}
