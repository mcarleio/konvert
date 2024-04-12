package io.mcarle.konvert.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference

interface SourceDataExtractionStrategy {

    val mappingCodeParentDeclaration: KSDeclaration
    val unitType: KSType

    fun extract(
        classDeclaration: KSClassDeclaration
    ): List<SourceData>

    sealed interface SourceData {
        val name: String
        val typeRef: KSTypeReference
    }

    data class SourceProperty(
        val property: KSPropertyDeclaration,
    ) : SourceData {
        override val name: String = property.simpleName.asString()
        override val typeRef: KSTypeReference = property.type
    }

    data class SourceGetter(
        val getter: KSFunctionDeclaration
    ) : SourceData {
        override val name: String = getter.simpleName.asString().removePrefix("get").replaceFirstChar { it.lowercase() }
        override val typeRef: KSTypeReference = getter.returnType!!
    }

}
