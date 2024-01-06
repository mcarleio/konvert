package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.toTypeName

data class KonverterInterface constructor(
    internal val kSClassDeclaration: KSClassDeclaration
) {
    val simpleName = kSClassDeclaration.simpleName.asString()
    val packageName = kSClassDeclaration.packageName.asString()
    val typeName = kSClassDeclaration.asStarProjectedType().toTypeName()
}

