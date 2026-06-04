package dev.mutwakil.version_catalog.parser

import dev.mutwakil.version_catalog.testutils.FakeTomlDocument
import dev.mutwakil.version_catalog.testutils.FakeTomlTable
import org.junit.Assert.assertEquals
import org.junit.Test

class VersionCatalogParserTest(){
    @Test
    fun parseVersions() {

        val versions =
            FakeTomlTable(
                keys = setOf("kotlin"),
                values = mapOf(
                    "kotlin" to "2.2.0"
                )
            )

        val document =
            FakeTomlDocument(
                mapOf(
                    "versions" to versions
                )
            )

        val catalog =
            VersionCatalogParserImpl()
                .parse(document)

        assertEquals(
            "2.2.0",
            catalog.versions["kotlin"]?.require
        )
    }
}