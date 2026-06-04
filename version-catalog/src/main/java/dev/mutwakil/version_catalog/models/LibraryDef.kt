package dev.mutwakil.version_catalog.models

data class LibraryDef(
    val alias: String,
    val group: String,
    val name: String,
    val version: String? = null,
    val versionRef: String? = null
)