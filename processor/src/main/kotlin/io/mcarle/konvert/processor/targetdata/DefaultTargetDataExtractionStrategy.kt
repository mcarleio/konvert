package io.mcarle.konvert.processor.targetdata

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration

class DefaultTargetDataExtractionStrategy : TargetDataExtractionStrategy {

    override fun extract(
        resolver: Resolver,
        classDeclaration: KSClassDeclaration,
        mappingCodeParentDeclaration: KSDeclaration
    ): TargetDataExtractionStrategy.TargetData {
        val primaryConstructor = if (classDeclaration.primaryConstructor?.isVisibleFrom(mappingCodeParentDeclaration) == true) {
            classDeclaration.primaryConstructor
        } else {
            null
        }

        val properties = classDeclaration.getAllProperties()
            .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }
            .map {
                if (it.isMutable) {
                    TargetDataExtractionStrategy.TargetVarProperty(it)
                } else {
                    TargetDataExtractionStrategy.TargetValProperty(it)
                }
            }

        val setters = classDeclaration.getAllFunctions()
            .filter { it.parameters.size == 1 }
            .filter { it.simpleName.asString().startsWith("set") }
            .filter { it.returnType == null }
            .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }
            .map { TargetDataExtractionStrategy.TargetSetter(it) }

        return TargetDataExtractionStrategy.TargetData(
            classDeclaration = classDeclaration,
            varProperties = properties.filterIsInstance<TargetDataExtractionStrategy.TargetVarProperty>().toList(),
            valProperties = properties.filterIsInstance<TargetDataExtractionStrategy.TargetValProperty>().toList(),
            setter = setters.toList(),
            primaryConstructor = primaryConstructor,
            constructors = classDeclaration.getConstructors()
                .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }
                .toList()
        )
    }
}
