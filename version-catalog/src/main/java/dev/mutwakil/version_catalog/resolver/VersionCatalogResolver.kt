package dev.mutwakil.version_catalog.resolver

import dev.mutwakil.version_catalog.models.VersionCatalog

class VersionCatalogResolver(
    private val catalog: VersionCatalog
) {

    fun resolveLibrary(
        accessor: String
    ): String? {

        val alias = accessor
            .removePrefix("libs.")
            .replace('.', '-')

        val library =
            catalog.libraries[alias]
                ?: return null

        val version =
            library.version
                ?: library.versionRef?.let {
                    catalog.versions[it]?.require
                }

        return buildString {
            append(library.group)
            append(':')
            append(library.name)

            if (version != null) {
                append(':')
                append(version)
            }
        }
    }
}