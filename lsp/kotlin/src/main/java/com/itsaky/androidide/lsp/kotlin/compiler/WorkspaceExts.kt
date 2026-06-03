package com.itsaky.androidide.lsp.kotlin.compiler

import com.itsaky.androidide.lsp.kotlin.compiler.modules.KtLibraryModule
import com.itsaky.androidide.lsp.kotlin.compiler.modules.KtModule
import com.itsaky.androidide.lsp.kotlin.compiler.modules.KtSourceModule
import com.itsaky.androidide.lsp.kotlin.compiler.modules.buildKtLibraryModule
import com.itsaky.androidide.lsp.kotlin.compiler.modules.buildKtSourceModule
import com.tyron.builder.BuildModule
import com.tyron.builder.project.api.AndroidModule
import org.jetbrains.kotlin.com.intellij.core.CoreApplicationEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

private val logger = LoggerFactory.getLogger("WorkspaceExts")

internal fun com.tyron.builder.project.Project.collectKtModules(
	project: Project,
	appEnv: CoreApplicationEnvironment
): List<KtModule> = buildList {
	fun addModule(module: KtModule) = add(module)

	val moduleProjects = modules
		.asSequence()
		.filterIsInstance<AndroidModule>()
//		.filter { it.path != rootProject.path }

	val jarToModMap = mutableMapOf<Path, KtLibraryModule>()

	fun addLibrary(path: Path): KtLibraryModule {
		val module = buildKtLibraryModule(project, appEnv) {
			id = path.pathString
			addContentRoot(path)
		}
		jarToModMap[path] = module
		return module
	}

	val bootClassPaths = mutableListOf<File>(BuildModule.getAndroidJar(), BuildModule.getLambdaStubs())
				.asSequence()
				.filter { it.exists() }
				.map { it.toPath() }
				.map(::addLibrary)

	val libraryDependencies = moduleProjects
		.flatMap { it.libraries }
		.filter { it.exists() }
		.map { it.toPath() }
		.associateWith(::addLibrary)

	val subprojectsAsModules = mutableMapOf<AndroidModule, KtSourceModule>()
	val sourceRootToModuleMap = mutableMapOf<Path, KtSourceModule>()

	fun getOrCreateModule(moduleProject: AndroidModule): KtSourceModule {
		subprojectsAsModules[moduleProject]?.let { return it }

		val module = buildKtSourceModule(project) {
			this.module = moduleProject

			bootClassPaths.forEach { addDependency(it) }

			moduleProject.libraries
				.forEach { classpath ->
					val libDep = libraryDependencies[classpath.toPath()]
					if (libDep == null) {
						logger.error(
							"Skipping non-existent classpath classpath: {}",
							classpath
						)
						return@forEach
					}
					addDependency(libDep)
				}

			moduleProject.subprojects.forEach { dep ->
				addDependency(getOrCreateModule(dep as AndroidModule))
			}
		}

		subprojectsAsModules[moduleProject] = module
		module.contentRoots.forEach { root -> sourceRootToModuleMap[root] = module }
		return module
	}

	moduleProjects.forEach { addModule(getOrCreateModule(it)) }
}
