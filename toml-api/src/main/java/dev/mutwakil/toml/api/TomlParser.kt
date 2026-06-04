package dev.mutwakil.toml.api

interface TomlParser {
    fun parse(text: String): TomlTable
}