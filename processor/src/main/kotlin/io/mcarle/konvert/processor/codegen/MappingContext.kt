package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

data class MappingContext constructor(
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val source: KSType,
    val target: KSType,
    val paramName: String?,
    val targetClassImportName: String?,
    /**
     * When set, the mapping writes into that already existing target instance instead of creating a new one.
     */
    val targetParamName: String? = null,
    /**
     * When true, the generated code returns the updated target instance.
     */
    val returnsTargetParam: Boolean = false,
)
