package io.mcarle.konvert.processor

import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType

class DefaultSourceDataExtractionStrategy(
    override val mappingCodeParentDeclaration: KSDeclaration,
    override val unitType: KSType
) : SourceDataExtractionStrategy {

    override fun extract(
        classDeclaration: KSClassDeclaration
    ): List<SourceDataExtractionStrategy.SourceData> {
        val properties = classDeclaration.getAllProperties()
            .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }
            .map { SourceDataExtractionStrategy.SourceProperty(it) }

        val getters = classDeclaration.getAllFunctions()
            .filter { it.parameters.isEmpty() }
            .filter { it.simpleName.asString().startsWith("get") }
            .filter { it.returnType != null }
            .filter { it.returnType?.resolve() != unitType }
            .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }
            .map { SourceDataExtractionStrategy.SourceGetter(it) }

        return (properties + getters).toList()
    }
}
