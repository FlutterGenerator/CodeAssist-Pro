package dev.mutwakil.version_catalog

import dev.mutwakil.version_catalog.models.LibraryDef

class VersionCatalogIndex {

    private val libraries =
        mutableMapOf<String, LibraryDef>()

    fun register(library: LibraryDef) {

        libraries[library.alias] = library

        libraries[
            "libs." +
                    library.alias.replace('-', '.')
        ] = library
    }
}