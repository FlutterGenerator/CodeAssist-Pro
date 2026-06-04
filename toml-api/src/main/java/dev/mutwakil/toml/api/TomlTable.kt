package dev.mutwakil.toml.api

interface TomlTable {

    val keys: Set<String>

    fun getString(key: String): String?

    fun getBoolean(key: String): Boolean?

    fun getTable(key: String): TomlTable?

    fun getArray(key: String): List<Any>?

    operator fun get(key: String): Any?
}