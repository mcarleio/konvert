package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.api.TypeConverterName
import io.mcarle.konvert.processor.sourcedata.SourceDataExtractionStrategy

data class PropertyMappingInfo constructor(
    val mappingParamName: String?,
    val sourceName: String?,
    val targetName: String,
    val constant: String?,
    val expression: String?,
    val ignore: Boolean,
    val enableConverters: List<TypeConverterName>,
    val sourceData: SourceDataExtractionStrategy.SourceData?,
    val isBasedOnAnnotation: Boolean,
    /**
     * Only set when mapping from several sources: how the value is read from its source parameter
     */
    val sourcePath: SourcePath? = null,
    /**
     * Only set when mapping from several sources: an [expression] then has no `it`, it reads the source parameters by name
     */
    val expressionWithoutReceiver: Boolean = false,
)

/**
 * A value read from one of several source parameters, possibly through further properties (`order.customer.name`).
 * Every step after a nullable one is read with `?.`, which makes [type] nullable.
 */
data class SourcePath constructor(
    /**
     * The code reading the value, e.g. `order?.customer?.name`
     */
    val code: String,
    val type: KSType,
)
