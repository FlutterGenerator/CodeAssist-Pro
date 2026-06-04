package dev.mutwakil.version_catalog.testutils

import dev.mutwakil.toml.api.TomlDocument
import dev.mutwakil.toml.api.TomlTable

class FakeTomlDocument(
    private val tables: Map<String, TomlTable>
) : TomlDocument {

    override fun getTable(
        name: String
    ): TomlTable? = tables[name]
}