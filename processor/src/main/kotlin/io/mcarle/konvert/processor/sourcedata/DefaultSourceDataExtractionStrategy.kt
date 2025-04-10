package io.mcarle.konvert.processor.sourcedata

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin


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

        val potentialFunctions = classDeclaration.getAllFunctions()
            .filter { it.parameters.isEmpty() }
            .filter { it.returnType != null }
            .filter { it.returnType?.resolve() != unitType }
            .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }

        val functionsAndGetters = when {
            classDeclaration.isRecord() -> handleRecords(potentialFunctions)
            else -> handleClasses(potentialFunctions, resolver)
        }

        return (properties + functionsAndGetters).toList()
    }

    private fun handleRecords(potentialFunctions: Sequence<KSFunctionDeclaration>): Sequence<SourceDataExtractionStrategy.SourceData> {
        return potentialFunctions
            .filter { !it.isConstructor() && !it.modifiers.contains(Modifier.ABSTRACT) }
            .map { SourceDataExtractionStrategy.SourceFunction(it) }
    }

    private fun handleClasses(
        potentialFunctions: Sequence<KSFunctionDeclaration>,
        resolver: Resolver
    ): Sequence<SourceDataExtractionStrategy.SourceData> {
        return potentialFunctions.mapNotNull {
            SourceDataExtractionStrategy.SourceData.from(it, resolver)
        }
    }

    private fun KSClassDeclaration.isRecord(): Boolean {
        return origin in listOf(
            Origin.JAVA,
            Origin.JAVA_LIB
        ) &&
            superTypes.any {
                it.resolve().declaration.qualifiedName?.asString() == "java.lang.Record"
            }
    }
}
