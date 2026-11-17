package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.IgnoredTargetPolicy
import io.mcarle.konvert.converter.api.config.InvalidMappingStrategy
import io.mcarle.konvert.converter.api.config.NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION
import io.mcarle.konvert.converter.api.config.NonConstructorPropertiesMapping
import io.mcarle.konvert.converter.api.config.enableConverters
import io.mcarle.konvert.converter.api.config.ignoredTargetPolicy
import io.mcarle.konvert.converter.api.config.invalidMappingStrategy
import io.mcarle.konvert.converter.api.config.nonConstructorPropertiesMapping
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.exceptions.IgnoredTargetException
import io.mcarle.konvert.processor.exceptions.InvalidMappingException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import io.mcarle.konvert.processor.sourcedata.DefaultSourceDataExtractionStrategy
import io.mcarle.konvert.processor.sourcedata.SourceDataExtractionStrategy
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
        visibilityContext: MappingVisibilityContext,
        additionalSourceParameters: List<KSValueParameter>
    ): CodeBlock {
        if (context.paramName != null && mappings.isEmpty()) {
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

        val sourceDataList = sourceDataExtractionStrategy.extract(resolver, context.sourceClassDeclaration, visibilityContext)
        val targetData = targetDataExtractionStrategy.extract(resolver, context.targetClassDeclaration, visibilityContext)

        val sourceProperties = PropertyMappingResolver(logger).determinePropertyMappings(
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

        verifyTargetExists(mappings, constructorParameters, targetData.varProperties, targetData.setter)

        reportIgnoredTargetsWithSource(
            context.functionName,
            mappings,
            sourceDataList,
            additionalSourceParameters,
            constructorParameters,
            targetData
        )

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
    }

    private fun verifyTargetExists(
        mappings: List<Mapping>,
        constructorParameters: List<KSValueParameter>,
        varProperties: List<TargetDataExtractionStrategy.TargetVarProperty>,
        setter: List<TargetDataExtractionStrategy.TargetSetter>
    ) {
        val mappingsWithMissingTargets = mappings.filter {
            it.target !in varProperties.map { it.name }
                && it.target !in setter.map { it.name }
                && it.target !in constructorParameters.mapNotNull { it.name?.asString() }
        }
        if (mappingsWithMissingTargets.isNotEmpty()) {
            when (Configuration.invalidMappingStrategy) {
                InvalidMappingStrategy.WARN -> mappingsWithMissingTargets.forEach {
                    logger.warn("Ignoring the mapping $it as the target field '${it.target}' does not exist.")
                }
                InvalidMappingStrategy.FAIL -> throw InvalidMappingException.missingTarget(mappingsWithMissingTargets)
            }
        }
    }

    /**
     * An ignored target that a source property or an additional parameter would map to by name silently drops its value.
     * A source property that another mapping uses as its `source` is not considered, as it is mapped elsewhere.
     */
    private fun reportIgnoredTargetsWithSource(
        functionName: String,
        mappings: List<Mapping>,
        sourceDataList: List<SourceDataExtractionStrategy.SourceData>,
        additionalSourceParameters: List<KSValueParameter>,
        constructorParameters: List<KSValueParameter>,
        targetData: TargetDataExtractionStrategy.TargetData
    ) {
        val policy = Configuration.ignoredTargetPolicy
        if (policy == IgnoredTargetPolicy.IGNORE) return

        val sourcesOfOtherMappings = mappings.filter { !it.ignore && it.source.isNotEmpty() }.map { it.source }.toSet()
        val sourcesByTarget = mappings
            .filter { it.ignore && it.source.isEmpty() }
            .mapNotNull { mapping ->
                val targetType = targetTypeOf(mapping.target, constructorParameters, targetData)?.resolve()
                    ?: return@mapNotNull null
                val parameter = additionalSourceParameters.firstOrNull {
                    it.name?.asString() == mapping.target && targetType.isAssignableFrom(it.type.resolve())
                }
                val sourceData = sourceDataList.firstOrNull {
                    it.name == mapping.target && it.name !in sourcesOfOtherMappings && isConvertible(it.typeRef.resolve(), targetType)
                }
                when {
                    parameter != null -> mapping.target to "parameter '${mapping.target}'"
                    sourceData != null -> mapping.target to "source property '${sourceData.name}'"
                    else -> null
                }
            }
            .toMap()
        if (sourcesByTarget.isEmpty()) return

        if (policy == IgnoredTargetPolicy.ERROR) {
            throw IgnoredTargetException(functionName, sourcesByTarget)
        }
        sourcesByTarget.forEach { (target, source) ->
            logger.warn(
                "$functionName ignores the target '$target', but the $source would map to it: " +
                    "remove @Mapping(target = \"$target\", ignore = true), or map it"
            )
        }
    }

    private fun targetTypeOf(
        target: String,
        constructorParameters: List<KSValueParameter>,
        targetData: TargetDataExtractionStrategy.TargetData
    ): KSTypeReference? {
        return constructorParameters.firstOrNull { it.name?.asString() == target }?.type
            ?: targetData.varProperties.firstOrNull { it.name == target }?.property?.type
            ?: targetData.setter.firstOrNull { it.name == target }?.typeRef
    }

    private fun isConvertible(sourceType: KSType, targetType: KSType): Boolean {
        return TypeConverterRegistry.withAdditionallyEnabledConverters(Configuration.enableConverters) {
            any { it.matches(sourceType, targetType) }
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
                    logger.logging(
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
                    NonConstructorPropertiesMapping.AUTO -> throw IllegalStateException("Did not resolve mapping mode ${NonConstructorPropertiesMapping.AUTO}")
                    NonConstructorPropertiesMapping.ALL,
                    NonConstructorPropertiesMapping.IMPLICIT -> sourceProperties.firstOrNull { it.targetName == variable.name }
                    NonConstructorPropertiesMapping.EXPLICIT -> sourceProperties.firstOrNull { it.targetName == variable.name && it.isBasedOnAnnotation }
                }
            }

        val missingMappings = propertiesToSourceProperty.filterValues { it == null }
        if (effectiveMappingMode == NonConstructorPropertiesMapping.ALL && missingMappings.isNotEmpty()) {
            val missingMappingsForProperties = missingMappings.map { it.key.name }.joinToString()
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
                    NonConstructorPropertiesMapping.AUTO -> throw IllegalStateException("Did not resolve mapping mode ${NonConstructorPropertiesMapping.AUTO}")
                    NonConstructorPropertiesMapping.ALL,
                    NonConstructorPropertiesMapping.IMPLICIT -> sourceProperties.firstOrNull { it.targetName == setter.name }
                    NonConstructorPropertiesMapping.EXPLICIT -> sourceProperties.firstOrNull { it.targetName == setter.name && it.isBasedOnAnnotation }
                }
            }

        val missingMappings = settersToSourceProperty.filterValues { it == null }
        if (effectiveMappingMode == NonConstructorPropertiesMapping.ALL && missingMappings.isNotEmpty()) {
            val missingMappingsForSetters = missingMappings.map { it.key.name }.joinToString()
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
