package io.mcarle.kmap.converter.api

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.Nullability

fun KSType.isNullable(): Boolean {
    return this.isMarkedNullable || this.nullability == Nullability.NULLABLE || this.nullability == Nullability.PLATFORM
}

fun KSType.classDeclaration(): KSClassDeclaration? = when (this.declaration) {
    is KSTypeAlias -> (this.declaration as KSTypeAlias).type.resolve().classDeclaration()
    is KSClassDeclaration -> this.declaration as KSClassDeclaration
    else -> null
}