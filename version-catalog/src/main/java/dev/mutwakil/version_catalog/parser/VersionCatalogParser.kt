package dev.mutwakil.version_catalog.parser

import dev.mutwakil.toml.api.TomlDocument
import dev.mutwakil.version_catalog.models.VersionCatalog

interface VersionCatalogParser {
    fun parse(document: TomlDocument): VersionCatalog
}