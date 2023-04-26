package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import io.mcarle.konvert.api.TypeConverterName

data class PropertyMappingInfo constructor(
    val mappingParamName: String?,
    val sourceName: String?,
    val targetName: String,
    val constant: String?,
    val expression: String?,
    val ignore: Boolean,
    val nullable: Boolean,
    val enableConverters: List<TypeConverterName>,
    val declaration: KSPropertyDeclaration?,
    val isBasedOnAnnotation: Boolean
)
