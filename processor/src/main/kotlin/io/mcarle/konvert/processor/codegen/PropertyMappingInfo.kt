package io.mcarle.konvert.processor.codegen

import io.mcarle.konvert.api.TypeConverterName
import io.mcarle.konvert.processor.SourceDataExtractionStrategy

data class PropertyMappingInfo constructor(
    val mappingParamName: String?,
    val sourceName: String?,
    val targetName: String,
    val constant: String?,
    val expression: String?,
    val ignore: Boolean,
    val enableConverters: List<TypeConverterName>,
    val sourceData: SourceDataExtractionStrategy.SourceData?,
    val isBasedOnAnnotation: Boolean
)
