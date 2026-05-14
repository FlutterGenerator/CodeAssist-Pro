package com.tyron.builder

import android.util.Log
import com.tyron.builder.project.api.Module
import com.tyron.common.Prefs
import java.io.File

/**
 * @author Mutwakil Suliman (Wadamzmail)
 **/
object TempTransformer {

    val TAG = TempTransformer::class.java.simpleName

    val CACHE_DIR: File by lazy {
        Prefs.getContext().cacheDir
    }

    val PLUGINS_TEMP_DIR: File by lazy {
        File(CACHE_DIR, "temp/plugins_temp")
    }

    fun cleanDir(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (file.deleteRecursively()) {
                Log.i(TAG, "deleted: $file")
            } else {
                Log.i(TAG, "delete failed: $file")
            }
        }
    }

    fun cleanPluginsTempForModule(module: Module) {
        cleanDir(getModulePluginsTempDir(module))
    }

    fun getModulePluginsTempDir(module: Module): File {
        return File(
            PLUGINS_TEMP_DIR, "${
                module.moduleName
                    .replace(":", "/")
                    .removePrefix("/")
            }/plugins_temp"
        )
    }

    fun transformPlugins(module: Module) {

        val pluginsDir = File(module.buildDirectory, "plugins")

        val pluginsTempDir =
            getModulePluginsTempDir(module)

        pluginsTempDir.mkdirs()

        pluginsDir.listFiles { file ->
            file.extension == "jar"
        }?.forEach { file ->

            val targetFile = File(pluginsTempDir, file.name)

            file.copyTo(targetFile, overwrite = true)

            Log.i(TAG, "file copied: $targetFile")
        }
    }

    fun getPlugins(module: Module): MutableList<File> {
        return getModulePluginsTempDir(module)
            .listFiles { file ->
                file.extension == "jar"
            }
            ?.toMutableList()
            ?: mutableListOf()
    }
}