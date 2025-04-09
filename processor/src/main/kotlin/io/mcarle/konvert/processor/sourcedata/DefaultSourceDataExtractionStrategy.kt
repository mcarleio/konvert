package io.mcarle.konvert.processor.sourcedata

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import io.mcarle.konvert.converter.api.isNullable


class DefaultSourceDataExtractionStrategy : SourceDataExtractionStrategy {

    override fun extract(
        resolver: Resolver,
        classDeclaration: KSClassDeclaration,
        mappingCodeParentDeclaration: KSDeclaration,
    ): List<SourceDataExtractionStrategy.SourceData> {
        val unitType = resolver.builtIns.unitType
        val booleanType = resolver.builtIns.booleanType

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
            else -> handleClasses(potentialFunctions, booleanType)
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
        booleanType: KSType
    ): Sequence<SourceDataExtractionStrategy.SourceData> {
        val getters = potentialFunctions
            .filter {
                it.simpleName.asString().run { startsWith("get") && !this[3].isLowerCase() }
            }
            .map { SourceDataExtractionStrategy.SourceGetter(it) }

        val functions = potentialFunctions
            .filter {
                it.simpleName.asString().run { startsWith("is") && !this[2].isLowerCase() }
                    && it.returnType!!.resolve().run { this == booleanType && !isNullable() }
            }
            .map { SourceDataExtractionStrategy.SourceFunction(it) }

        return getters + functions
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
