package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

data class MappingContext constructor(
    /**
     * The objects to map from: exactly one, except for a `@Konverter` function with several `@Konverter.Source` parameters
     */
    val sources: List<MappingSource>,
    val targetClassDeclaration: KSClassDeclaration,
    val target: KSType,
    val targetClassImportName: String?,
) {
    init {
        require(sources.isNotEmpty()) { "A mapping needs at least one source" }
    }

    val hasMultipleSources: Boolean get() = sources.size > 1
}

data class MappingSource constructor(
    val classDeclaration: KSClassDeclaration,
    val type: KSType,
    /**
     * The name of the function parameter holding the source, or `null` when the source is the receiver (`@KonvertTo`)
     */
    val paramName: String?,
)
