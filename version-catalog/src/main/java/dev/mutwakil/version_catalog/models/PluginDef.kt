package dev.mutwakil.version_catalog.models

data class PluginDef(
    val alias: String,
    val id: String,
    val version: String? = null,
    val versionRef: String? = null
)