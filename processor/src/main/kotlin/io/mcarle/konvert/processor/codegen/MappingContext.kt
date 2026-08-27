package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.mcarle.konvert.processor.ResolvedType

data class MappingContext constructor(
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val source: ResolvedType,
    val target: ResolvedType,
    val paramName: String?,
    val targetClassImportName: String?,
)
