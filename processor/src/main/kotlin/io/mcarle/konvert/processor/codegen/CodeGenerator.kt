package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.enforceNotNull
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.exceptions.KonvertException
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import io.mcarle.konvert.processor.sourcedata.DefaultSourceDataExtractionStrategy
import io.mcarle.konvert.processor.targetdata.DefaultTargetDataExtractionStrategy

class CodeGenerator constructor(
    private val logger: KSPLogger,
    private val resolver: Resolver
) {

    private val sourceDataExtractionStrategy = DefaultSourceDataExtractionStrategy()
    private val targetDataExtractionStrategy = DefaultTargetDataExtractionStrategy()

    fun generateCode(
        mappings: List<Mapping>,
        enforcedConstructorTypes: List<KSClassDeclaration>,
        context: MappingContext,
        mappingCodeParentDeclaration: KSDeclaration,
        additionalSourceParameters: List<KSValueParameter>
    ): CodeBlock {
        try {
            if (context.paramName != null) {
                val existingTypeConverter = TypeConverterRegistry
                    .firstOrNull {
                        it.matches(context.source, context.target) && it !is AnnotatedConverter
                    }

                if (existingTypeConverter != null) {
                    return CodeBlock.of(
                        "return·%L",
                        existingTypeConverter.convert(context.paramName, context.source, context.target)
                    )
                }
            }

            if (context.source.isNullable() && !context.target.isNullable() && !Configuration.enforceNotNull) {
                throw NotNullOperatorNotEnabledException(context.paramName, context.source, context.target)
            }

            val sourceClassDeclaration = context.source.classDeclaration()!!
            val targetClassDeclaration = context.target.classDeclaration()!!

            val sourceDataList = sourceDataExtractionStrategy.extract(resolver, sourceClassDeclaration, mappingCodeParentDeclaration)
            val targetData = targetDataExtractionStrategy.extract(resolver, targetClassDeclaration, mappingCodeParentDeclaration)

            val sourceProperties = PropertyMappingResolver.determinePropertyMappings(
                mappingParamName = context.paramName,
                mappings = mappings,
                additionalSourceParameters = additionalSourceParameters,
                sourceDataList = sourceDataList
            )

            val constructor = ConstructorResolver.determineConstructor(
                targetData = targetData,
                sourceProperties = sourceProperties,
                constructorTypes = enforcedConstructorTypes
            )

            val constructorParameters = constructor.parameters

            verifyAvailableMappingsForConstructorParameters(sourceProperties, constructorParameters)

            val constructorMatchesExactly = propertiesMatchingExact(sourceProperties, constructorParameters)

            val variablesWithoutConstructorParameters = if (constructorMatchesExactly) {
                emptyList()
            } else {
                targetData.varProperties
                    .filter { variable ->
                        variable.name !in constructorParameters.mapNotNull { it.name?.asString() }
                    }
            }

            val remainingSetters = if (constructorMatchesExactly) {
                emptyList()
            } else {
                targetData.setter
                    .filter { setter ->
                        setter.name !in constructorParameters.mapNotNull { it.name?.asString() }
                            && setter.name !in variablesWithoutConstructorParameters.map { it.name }
                    }
            }

            return MappingCodeGenerator().generateMappingCode(
                context,
                sourceProperties.sortedByDescending { it.isBasedOnAnnotation },
                constructor,
                variablesWithoutConstructorParameters.map { it.property },
                remainingSetters
            )
        } catch (e: Exception) {
            throw KonvertException(context.source, context.target, e)
        }
    }

    private fun verifyAvailableMappingsForConstructorParameters(
        sourceProperties: List<PropertyMappingInfo>,
        constructorParameters: List<KSValueParameter>
    ) {
        val availableNotIgnoredTargetNames = sourceProperties
            .filterNot { it.ignore }
            .map { it.targetName }

        val missingSourceForRequiredParameter = constructorParameters.firstOrNull {
            if (it.hasDefault) {
                false
            } else if (it.type.resolve().isNullable()) {
                false
            } else {
                it.name?.asString() !in availableNotIgnoredTargetNames
            }
        }
        if (missingSourceForRequiredParameter != null) {
            throw PropertyMappingNotExistingException(missingSourceForRequiredParameter, sourceProperties)
        }
    }

    private fun propertiesMatchingExact(props: List<PropertyMappingInfo>, parameters: List<KSValueParameter>): Boolean {
        if (parameters.isEmpty()) return props.isEmpty()
        return props
            .filter { it.isBasedOnAnnotation }
            .filterNot { it.ignore }
            .all { property ->
                parameters.any { parameter ->
                    property.targetName == parameter.name?.asString()
                }
            }
    }

    class TargetElement private constructor(
        val property: KSPropertyDeclaration? = null,
        val parameter: KSValueParameter? = null,
        val setter: KSFunctionDeclaration? = null,
    ) {
        constructor(annotated: KSAnnotated) : this(
            property = annotated as? KSPropertyDeclaration,
            parameter = annotated as? KSValueParameter,
            setter = annotated as? KSFunctionDeclaration,
        )

        override fun toString(): String {
            return when {
                property != null -> property.simpleName.asString()
                parameter != null -> parameter.name?.asString() ?: parameter.toString()
                setter != null -> setter.simpleName.asString()
                else -> error("No property, parameter or setter available")
            }
        }

    }

}
