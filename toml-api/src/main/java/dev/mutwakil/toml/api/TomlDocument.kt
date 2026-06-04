package dev.mutwakil.toml.api

interface TomlDocument {
    fun getTable(name: String): TomlTable?
}