package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSType

data class MappingContext(
    val source: KSType,
    val target: KSType,
    val paramName: String?,
    val targetClassImportName: String?,
)
