package com.itsaky.androidide.compose.preview.data.source

import com.tyron.builder.BuildModule
import com.tyron.builder.project.IProjectManager
import com.tyron.builder.project.impl.AndroidModuleImpl
import com.tyron.builder.project.impl.getCompileClasspaths
import com.tyron.builder.project.impl.getIntermediateClasspaths
import com.tyron.builder.project.impl.getRuntimeDexFiles
import org.slf4j.LoggerFactory
import java.io.File

data class ProjectContext(
    val modulePath: String?,
    val variantName: String,
    val compileClasspaths: List<File>,
    val intermediateClasspaths: Set<File>,
    val projectDexFiles: List<File>,
    val needsBuild: Boolean
)

class ProjectContextSource {

    fun resolveContext(filePath: String): ProjectContext {
        if (filePath.isBlank()) {
            LOG.info("Empty file path, returning default context")
            return ProjectContext(
                modulePath = null,
                variantName = "debug",
                compileClasspaths = emptyList(),
                intermediateClasspaths = emptySet(),
                projectDexFiles = emptyList(),
                needsBuild = false
            )
        }

        val file = File(filePath)
        LOG.info("Resolving project context for file: {}", file.absolutePath)

        val projectManager = IProjectManager.getInstance()
        val module = projectManager.currentProject.getModule(file) as AndroidModuleImpl

        if (module == null) {
            LOG.info("No module found for file")
            return ProjectContext(
                modulePath = null,
                variantName = "debug",
                compileClasspaths = emptyList(),
                intermediateClasspaths = emptySet(),
                projectDexFiles = emptyList(),
                needsBuild = false
            )
        }

        LOG.info("Found module: {} (type: {})", module.name, module.javaClass.simpleName)

        val intermediateClasspaths = module.getIntermediateClasspaths()
        val compileClasspaths = (module.getCompileClasspaths() + intermediateClasspaths+ listOf(
            BuildModule.getAndroidJar(), BuildModule.getLambdaStubs())).distinct()

        val projectDexFiles = module.getRuntimeDexFiles().toList()
        val variantName = "debug" //(module as? AndroidModule)?.getSelectedVariant()?.name ?: "debug"
        val needsBuild = intermediateClasspaths.isEmpty()

        LOG.info("Found {} total classpaths ({} compile, {} intermediate) for module: {}",
            compileClasspaths.size,
            compileClasspaths.size - intermediateClasspaths.size,
            intermediateClasspaths.size,
            module.name)
        LOG.info("Found {} project DEX files for runtime loading", projectDexFiles.size)
        LOG.info("Module path: {}, variant: {}, needsBuild: {}", module.moduleName, variantName, needsBuild)

        if (!needsBuild) {
            intermediateClasspaths.forEach { cp ->
                LOG.info("  Intermediate: {} (exists: {})", cp.absolutePath, cp.exists())
            }
            projectDexFiles.forEach { dex ->
                LOG.info("  Project DEX: {} (exists: {})", dex.absolutePath, dex.exists())
            }
        }

        return ProjectContext(
            modulePath = module.moduleName,
            variantName = variantName,
            compileClasspaths = compileClasspaths,
            intermediateClasspaths = intermediateClasspaths,
            projectDexFiles = projectDexFiles,
            needsBuild = needsBuild
        )
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ProjectContextSource::class.java)
    }
}
