package io.mcarle.konvert.processor.sourcedata

import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration

class DefaultSourceDataExtractionStrategy : SourceDataExtractionStrategy {

    override fun extract(
        resolver: Resolver,
        classDeclaration: KSClassDeclaration,
        mappingCodeParentDeclaration: KSDeclaration,
    ): List<SourceDataExtractionStrategy.SourceData> {
        val unitType = resolver.builtIns.unitType

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
