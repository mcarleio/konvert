package io.mcarle.konvert.processor.sourcedata

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import io.mcarle.konvert.processor.ResolvedType
import io.mcarle.konvert.processor.TypeSubstitution
import io.mcarle.konvert.processor.codegen.MappingVisibilityContext
import io.mcarle.konvert.processor.codegen.isVisibleFrom


class DefaultSourceDataExtractionStrategy : SourceDataExtractionStrategy {

    override fun extract(
        resolver: Resolver,
        sourceType: ResolvedType,
        classDeclaration: KSClassDeclaration,
        visibilityContext: MappingVisibilityContext,
    ): List<SourceDataExtractionStrategy.SourceData> {
        val unitType = resolver.builtIns.unitType

        val substitution by lazy { TypeSubstitution.of(sourceType, resolver) }

        val properties = classDeclaration.getAllProperties()
            .filter { it.isVisibleFrom(visibilityContext) }
            .map { SourceDataExtractionStrategy.SourceProperty(it, substitution, resolver) }

        val potentialFunctions = classDeclaration.getAllFunctions()
            .filter { it.parameters.isEmpty() }
            .filter { it.returnType != null }
            .filter { it.returnType?.resolve() != unitType }
            .filter { it.isVisibleFrom(visibilityContext) }

        val functionsAndGetters = when {
            classDeclaration.isRecord() -> handleRecords(potentialFunctions, substitution, resolver)
            else -> handleClasses(potentialFunctions, substitution, resolver)
        }

        return (properties + functionsAndGetters).toList()
    }

    private fun handleRecords(
        potentialFunctions: Sequence<KSFunctionDeclaration>,
        substitution: TypeSubstitution,
        resolver: Resolver
    ): Sequence<SourceDataExtractionStrategy.SourceData> {
        return potentialFunctions
            .filter { !it.isConstructor() && !it.modifiers.contains(Modifier.ABSTRACT) }
            .map { SourceDataExtractionStrategy.SourceFunction(it, substitution, resolver) }
    }

    private fun handleClasses(
        potentialFunctions: Sequence<KSFunctionDeclaration>,
        substitution: TypeSubstitution,
        resolver: Resolver
    ): Sequence<SourceDataExtractionStrategy.SourceData> {
        return potentialFunctions.mapNotNull {
            SourceDataExtractionStrategy.SourceData.from(it, substitution, resolver)
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
