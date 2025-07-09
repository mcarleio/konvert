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
import io.mcarle.konvert.converter.api.config.NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION
import io.mcarle.konvert.converter.api.config.NonConstructorPropertiesMapping
import io.mcarle.konvert.converter.api.config.enforceNotNull
import io.mcarle.konvert.converter.api.config.nonConstructorPropertiesMapping
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.exceptions.KonvertException
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import io.mcarle.konvert.processor.sourcedata.DefaultSourceDataExtractionStrategy
import io.mcarle.konvert.processor.targetdata.DefaultTargetDataExtractionStrategy
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy

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

            val sourceDataList =
                sourceDataExtractionStrategy.extract(resolver, context.sourceClassDeclaration, mappingCodeParentDeclaration)
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

            val effectiveNonConstructorPropertiesMappingMode = determineNonConstructorPropertiesMappingMode(
                targetData,
                sourceProperties
            )

            val variablesWithoutConstructorParameters = obtainTargetNonConstructorProperties(
                sourceProperties,
                constructorParameters,
                targetData,
                effectiveNonConstructorPropertiesMappingMode
            )

            val remainingSetters = obtainTargetNonConstructorSetters(
                variablesWithoutConstructorParameters,
                sourceProperties,
                constructorParameters,
                targetData,
                effectiveNonConstructorPropertiesMappingMode
            )

            return MappingCodeGenerator(logger).generateMappingCode(
                context,
                sourceProperties.sortedByDescending { it.isBasedOnAnnotation },
                constructor,
                variablesWithoutConstructorParameters.map { it.property },
                remainingSetters.toList()
            )
        } catch (e: Exception) {
            throw KonvertException(context.source, context.target, e)
        }
    }

    /**
     * Determines which non-constructor properties and setters mapping mode
     */
    private fun determineNonConstructorPropertiesMappingMode(
        targetData: TargetDataExtractionStrategy.TargetData,
        sourceProperties: List<PropertyMappingInfo>,
    ): NonConstructorPropertiesMapping {
        return when (val value = Configuration.nonConstructorPropertiesMapping) {
            NonConstructorPropertiesMapping.AUTO -> {
                val userDefinedProperties = sourceProperties.filter { it.isBasedOnAnnotation }

                return if (userDefinedProperties.none { !it.ignore }) {
                    NonConstructorPropertiesMapping.IMPLICIT
                } else {
                    NonConstructorPropertiesMapping.EXPLICIT
                }.also {
                    logger.info(
                        "${NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key} resolved to: $it",
                        targetData.classDeclaration
                    )
                }
            }
            NonConstructorPropertiesMapping.IMPLICIT,
            NonConstructorPropertiesMapping.EXPLICIT,
            NonConstructorPropertiesMapping.ALL -> value
        }
    }

    /**
     * Determines which non-constructor properties should be included in the mapping.
     */
    private fun obtainTargetNonConstructorProperties(
        sourceProperties: List<PropertyMappingInfo>,
        constructorParameters: List<KSValueParameter>,
        targetData: TargetDataExtractionStrategy.TargetData,
        effectiveMappingMode: NonConstructorPropertiesMapping
    ): Set<TargetDataExtractionStrategy.TargetVarProperty> {

        val remainingProperties = targetData.varProperties
            .filter { variable ->
                variable.name !in constructorParameters.mapNotNull { it.name?.asString() }
            }

        val propertiesToSourceProperty = remainingProperties
            .associateWith { variable ->
                when (effectiveMappingMode) {
                    NonConstructorPropertiesMapping.AUTO -> throw RuntimeException("Did not resolve mapping mode ${NonConstructorPropertiesMapping.AUTO}")
                    NonConstructorPropertiesMapping.ALL,
                    NonConstructorPropertiesMapping.IMPLICIT -> sourceProperties.firstOrNull { it.targetName == variable.name }
                    NonConstructorPropertiesMapping.EXPLICIT -> sourceProperties.firstOrNull { it.targetName == variable.name && it.isBasedOnAnnotation }
                }
            }

        if (effectiveMappingMode == NonConstructorPropertiesMapping.ALL && propertiesToSourceProperty.containsValue(null)) {
            val missingMappingsForProperties = propertiesToSourceProperty.filterValues { it == null }.map { it.key.name }.joinToString()
            throw RuntimeException(
                "With `konvert.non-constructor-properties-mapping` being ${NonConstructorPropertiesMapping.ALL}, all target properties must have a matching source property. " +
                    "Missing mappings for properties: ${missingMappingsForProperties}."
            )
        }

        return propertiesToSourceProperty.filterValues { it != null && !it.ignore }.keys
    }

    /**
     * Determines which setters should be included in the mapping.
     */
    private fun obtainTargetNonConstructorSetters(
        mappedNonConstructorProperties: Set<TargetDataExtractionStrategy.TargetVarProperty>,
        sourceProperties: List<PropertyMappingInfo>,
        constructorParameters: List<KSValueParameter>,
        targetData: TargetDataExtractionStrategy.TargetData,
        effectiveMappingMode: NonConstructorPropertiesMapping
    ): Set<TargetDataExtractionStrategy.TargetSetter> {

        val remainingSetters = targetData.setter
            .filter { setter ->
                setter.name !in constructorParameters.mapNotNull { it.name?.asString() }
                    && setter.name !in mappedNonConstructorProperties.map { it.name }
            }

        val settersToSourceProperty = remainingSetters
            .associateWith { setter ->
                when (effectiveMappingMode) {
                    NonConstructorPropertiesMapping.AUTO -> throw RuntimeException("Did not resolve mapping mode ${NonConstructorPropertiesMapping.AUTO}")
                    NonConstructorPropertiesMapping.ALL,
                    NonConstructorPropertiesMapping.IMPLICIT -> sourceProperties.firstOrNull { it.targetName == setter.name }
                    NonConstructorPropertiesMapping.EXPLICIT -> sourceProperties.firstOrNull { it.targetName == setter.name && it.isBasedOnAnnotation }
                }
            }

        if (effectiveMappingMode == NonConstructorPropertiesMapping.ALL && settersToSourceProperty.containsValue(null)) {
            val missingMappingsForSetters = settersToSourceProperty.filterValues { it == null }.map { it.key.name }.joinToString()
            throw RuntimeException(
                "With `konvert.non-constructor-properties-mapping` being ${NonConstructorPropertiesMapping.ALL}, all target setters must have a matching source property. " +
                    "Missing mappings for setters: ${missingMappingsForSetters}."
            )
        }
        return settersToSourceProperty.filterValues { it != null && !it.ignore }.keys
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

}
