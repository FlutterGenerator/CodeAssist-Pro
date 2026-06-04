package dev.mutwakil.version_catalog.parser

import dev.mutwakil.toml.api.TomlDocument
import dev.mutwakil.version_catalog.models.VersionCatalog
import dev.mutwakil.version_catalog.models.VersionDef
import org.slf4j.LoggerFactory

class VersionCatalogParserImpl : VersionCatalogParser {


    companion object {
        private val LOG =
            LoggerFactory.getLogger(
                VersionCatalogParserImpl::class.java
            )
    }

    override fun parse(
        document: TomlDocument
    ): VersionCatalog {

        val catalog = VersionCatalog()

        parseVersions(document, catalog)
        parseLibraries(document, catalog)
        parsePlugins(document, catalog)
        parseBundles(document, catalog)

        return catalog
    }

    private fun parseVersions(
        document: TomlDocument,
        catalog: VersionCatalog
    ) {

        LOG.debug("Parsing versions section")

        val versionsTable =
            document.getTable("versions")
                ?: return

        for (alias in versionsTable.keys) {

            LOG.debug(
                "Found {} version definitions",
                versionsTable.keys.size
            )

            val value =
                versionsTable.getString(alias)

            if (value != null) {
                catalog.versions[alias] =
                    VersionDef(
                        require = value
                    )
            }
        }
    }

    private fun parseLibraries(
        document: TomlDocument,
        catalog: VersionCatalog
    ) {
        TODO("Not yet implemented")
    }

    private fun parsePlugins(
        document: TomlDocument,
        catalog: VersionCatalog
    ) {
        TODO("Not yet implemented")
    }

    private fun parseBundles(
        document: TomlDocument,
        catalog: VersionCatalog
    ) {
        TODO("Not yet implemented")
    }
}