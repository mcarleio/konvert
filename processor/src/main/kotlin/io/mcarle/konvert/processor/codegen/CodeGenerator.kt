package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.enforceNotNull
import io.mcarle.konvert.converter.api.config.ignoreUnmappedTargetProperties
import io.mcarle.konvert.converter.api.config.nonConstructorPropertiesMapping
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.exceptions.KonvertException
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import io.mcarle.konvert.processor.sourcedata.DefaultSourceDataExtractionStrategy
import io.mcarle.konvert.processor.targetdata.DefaultTargetDataExtractionStrategy
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy.TargetSetter
import kotlin.collections.List

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

            val sourceDataList = sourceDataExtractionStrategy.extract(resolver, context.sourceClassDeclaration, mappingCodeParentDeclaration)
            val targetData = targetDataExtractionStrategy.extract(resolver, context.targetClassDeclaration, mappingCodeParentDeclaration)

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

            val (variablesWithoutConstructorParameters, remainingSetters) = obtainTargetNonConstructorPropertiesAndSetters(sourceProperties, mappings, constructorParameters, targetData)

            return MappingCodeGenerator(logger).generateMappingCode(
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

    /**
     * Determines which non-constructor properties and setters should be included in the mapping.
     *
     * Applies configuration options to control inclusion and fallback logic in strict mode.
     */
    private fun obtainTargetNonConstructorPropertiesAndSetters(
        sourceProperties: List<PropertyMappingInfo>,
        mappings: List<Mapping>,
        constructorParameters: List<KSValueParameter>,
        targetData: TargetDataExtractionStrategy.TargetData
    ): Pair<List<TargetDataExtractionStrategy.TargetVarProperty>, List<TargetSetter>> {
        val constructorMatchesExactly = propertiesMatchingExact(sourceProperties, constructorParameters)
        return if (constructorMatchesExactly && Configuration.nonConstructorPropertiesMapping == "ignore") {
            // Default behavior: skip mapping non-constructor properties when constructor parameters match source
            Pair(emptyList(), emptyList())
        } else {
            val effectiveMappingMode = if (constructorParameters.isEmpty() && mappings.isEmpty() && Configuration.nonConstructorPropertiesMapping == "strict") {
                // fallback: target class has no constructor and no mappings are defined
                // in strict mode this would result in no mapping at all, which is not useful
                logger.warn(
                    "konvert.non-constructor-properties-mapping=strict is active, but target class `${targetData.classDeclaration.simpleName}` has no constructor and no @Mapping. Fallback to `auto` mode will be applied.",
                    targetData.classDeclaration
                )
                "auto"
            } else Configuration.nonConstructorPropertiesMapping
            // Map properties outside constructor and available public setters (if applicable)
            val nonConstructorVariables = targetData.varProperties
                .filter { variable ->
                    variable.name !in constructorParameters.mapNotNull { it.name?.asString() }
                }.filter { variable ->
                    effectiveMappingMode != "strict" || mappings.any { it.target == variable.name && !it.ignore }
                }
            val remainingSetters = targetData.setter
                .filter { setter ->
                    setter.name !in constructorParameters.mapNotNull { it.name?.asString() }
                        && setter.name !in nonConstructorVariables.map { it.name }
                }
            Pair(nonConstructorVariables, remainingSetters)
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
        if (missingSourceForRequiredParameter != null && !Configuration.ignoreUnmappedTargetProperties) {
            throw PropertyMappingNotExistingException(missingSourceForRequiredParameter, sourceProperties)
        }
    }

    private fun propertiesMatchingExact(props: List<PropertyMappingInfo>, parameters: List<KSValueParameter>): Boolean {
        if (parameters.isEmpty()) return props.isEmpty()
        val propsFiltered = props
            .filter { it.isBasedOnAnnotation }
            .filterNot { it.ignore }
        return propsFiltered
            .all { property ->
                parameters.any { parameter ->
                    property.targetName == parameter.name?.asString()
                }
            }
    }

}
