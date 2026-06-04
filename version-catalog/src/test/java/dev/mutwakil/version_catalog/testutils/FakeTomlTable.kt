package dev.mutwakil.version_catalog.testutils

import dev.mutwakil.toml.api.TomlTable

class FakeTomlTable(
    override val keys: Set<String>,
    private val values: Map<String, Any?>
) : TomlTable {

    override fun getString(
        key: String
    ): String? = values[key] as? String

    override fun getBoolean(
        key: String
    ): Boolean? = values[key] as? Boolean

    override fun getTable(
        key: String
    ): TomlTable? = values[key] as? TomlTable

    override fun getArray(
        key: String
    ): List<Any>? = values[key] as? List<Any>

    override fun get(
        key: String
    ): Any? = values[key]
}