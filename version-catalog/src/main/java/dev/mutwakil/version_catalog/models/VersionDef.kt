package dev.mutwakil.version_catalog.models

data class VersionDef(
    val require: String? = null,
    val prefer: String? = null,
    val strictly: String? = null
)