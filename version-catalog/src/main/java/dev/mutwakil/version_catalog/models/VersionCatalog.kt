package dev.mutwakil.version_catalog.models

data class VersionCatalog(
    val versions: MutableMap<String, VersionDef> = mutableMapOf(),
    val libraries: MutableMap<String, LibraryDef> = mutableMapOf(),
    val plugins: MutableMap<String, PluginDef> = mutableMapOf(),
    val bundles: MutableMap<String, BundleDef> = mutableMapOf()
)