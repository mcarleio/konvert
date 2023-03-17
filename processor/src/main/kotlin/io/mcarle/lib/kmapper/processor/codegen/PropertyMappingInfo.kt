package io.mcarle.lib.kmapper.processor.codegen

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import kotlin.reflect.KClass

data class PropertyMappingInfo constructor(
    val mappingParamName: String?,
    val sourceName: String?,
    val targetName: String,
    val constant: String?,
    val expression: String?,
    val ignore: Boolean,
    val enableConverters: List<KClass<out TypeConverter>>,
    val declaration: KSPropertyDeclaration?,
    val isBasedOnAnnotation: Boolean
)