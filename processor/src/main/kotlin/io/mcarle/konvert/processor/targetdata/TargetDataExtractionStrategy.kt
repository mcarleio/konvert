package io.mcarle.konvert.processor.targetdata

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.processor.ResolvedType
import io.mcarle.konvert.processor.TypeSubstitution
import io.mcarle.konvert.processor.codegen.MappingVisibilityContext
import io.mcarle.konvert.processor.resolveFully

fun interface TargetDataExtractionStrategy {

    fun extract(
        resolver: Resolver,
        targetType: ResolvedType,
        classDeclaration: KSClassDeclaration,
        visibilityContext: MappingVisibilityContext
    ): TargetData

    data class TargetData(
        val classDeclaration: KSClassDeclaration,
        val varProperties: List<TargetVarProperty>,
        val setter: List<TargetSetter>,
        val primaryConstructor: TargetConstructor?,
        val constructors: List<TargetConstructor>,
    )

    data class TargetVarProperty(
        private val property: KSPropertyDeclaration,
        private val substitution: TypeSubstitution,
        private val resolver: Resolver
    ) {
        val name = property.simpleName.asString()
        val type: ResolvedType by lazy { property.type.resolveFully(resolver, substitution) }
    }

    data class TargetConstructor(
        private val constructor: KSFunctionDeclaration,
        private val substitution: TypeSubstitution,
        private val resolver: Resolver
    ) {

        val origin = constructor.origin
        val className = constructor.parentDeclaration!!.simpleName.asString()
        val parameters = constructor.parameters.map {
            TargetConstructorParameter(it, substitution, resolver)
        }
    }

    data class TargetConstructorParameter(
        private val valueParameter: KSValueParameter,
        private val substitution: TypeSubstitution,
        private val resolver: Resolver
    ) {
        val name = valueParameter.name?.asString()
        val hasDefault = valueParameter.hasDefault
        val type: ResolvedType by lazy { valueParameter.type.resolveFully(resolver, substitution) }

        fun valueParameterToString() = valueParameter.toString()
    }

    data class TargetSetter(
        private val setter: KSFunctionDeclaration,
        private val correspondingGetter: KSFunctionDeclaration?,
        private val substitution: TypeSubstitution,
        private val resolver: Resolver
    ) {
        companion object {
            fun extractPropertyName(setter: KSFunctionDeclaration): String {
                return setter.simpleName.asString().removePrefix("set").replaceFirstChar { it.lowercase() }
            }
        }

        val name = extractPropertyName(setter)
        val type: ResolvedType by lazy { setter.parameters.single().type.resolveFully(resolver, substitution) }

        fun generateAssignmentCode(valueToAssign: CodeBlock): CodeBlock {
            return if (correspondingGetter != null) {
                if (correspondingGetter.simpleName.asString().startsWith("is")) {
                    CodeBlock.of("${correspondingGetter.simpleName.asString()}·=·%L", valueToAssign)
                } else {
                    CodeBlock.of("$name·=·%L", valueToAssign)
                }
            } else {
                CodeBlock.of("${setter.simpleName.asString()}(%L)", valueToAssign)
            }
        }
    }

}
