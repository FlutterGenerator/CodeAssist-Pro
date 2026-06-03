package com.tyron.builder.project.api

import org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule

open class KtJavaModule(
    override val name: String,
    override val moduleRoots: List<JavaModule.Root>,
    override val moduleInfoFile: VirtualFile?,
    override val isSourceModule: Boolean
) : JavaModule {
    override fun exports(packageFqName: FqName): Boolean {
       return true
    }

    override fun exportsTo(
        packageFqName: FqName,
        moduleName: String
    ): Boolean {
       return true
    }

}