package io.mcarle.konvert.processor.targetdata

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

fun interface TargetDataExtractionStrategy {

    fun extract(
        resolver: Resolver,
        classDeclaration: KSClassDeclaration,
        mappingCodeParentDeclaration: KSDeclaration
    ): TargetData

    data class TargetData(
        val classDeclaration: KSClassDeclaration,
        val varProperties: List<TargetVarProperty>,
        val valProperties: List<TargetValProperty>,
        val setter: List<TargetSetter>,
        val primaryConstructor: KSFunctionDeclaration?,
        val constructors: List<KSFunctionDeclaration>,
    )

    data class TargetVarProperty(
        val property: KSPropertyDeclaration,
    ) {
        val name = property.simpleName.asString()
        val typeRef = property.type
    }

    data class TargetValProperty(
        val property: KSPropertyDeclaration,
    ) {
        val name = property.simpleName.asString()
        val typeRef = property.type
    }

    data class TargetSetter(
        val setter: KSFunctionDeclaration
    ) {
        val name = setter.simpleName.asString().removePrefix("set").replaceFirstChar { it.lowercase() }
        val typeRef = setter.parameters.single().type
    }

}
