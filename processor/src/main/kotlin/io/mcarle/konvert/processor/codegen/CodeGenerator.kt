package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.InvalidMappingStrategy
import io.mcarle.konvert.converter.api.config.NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION
import io.mcarle.konvert.converter.api.config.NonConstructorPropertiesMapping
import io.mcarle.konvert.converter.api.config.UnmappedTargetPolicy
import io.mcarle.konvert.converter.api.config.invalidMappingStrategy
import io.mcarle.konvert.converter.api.config.nonConstructorPropertiesMapping
import io.mcarle.konvert.converter.api.config.unmappedTargetPolicy
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.exceptions.InvalidMappingException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import io.mcarle.konvert.processor.exceptions.UnmappedTargetException
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

        reportUnmappedTargets(
            constructor,
            sourceProperties,
            targetData,
            variablesWithoutConstructorParameters.map { it.name } + remainingSetters.map { it.name }
        )

        return MappingCodeGenerator(logger).generateMappingCode(
            context,
            sourceProperties.sortedByDescending { it.isBasedOnAnnotation },
            constructor,
            variablesWithoutConstructorParameters.map { it.property },
            remainingSetters.toList()
        )
    }

    /**
     * A constructor parameter that no source property and no mapping provides silently takes its default value, or
     * `null`. An assignable non-constructor property that is neither written nor excluded with `ignore = true` silently
     * keeps the value the constructor gives it, whichever [NonConstructorPropertiesMapping] left it unwritten.
     */
    private fun reportUnmappedTargets(
        constructor: KSFunctionDeclaration,
        sourceProperties: List<PropertyMappingInfo>,
        targetData: TargetDataExtractionStrategy.TargetData,
        writtenNonConstructorNames: List<String>
    ) {
        val policy = Configuration.unmappedTargetPolicy
        if (policy == UnmappedTargetPolicy.IGNORE) return

        val parameterNames = constructor.parameters.mapNotNull { it.name?.asString() }
        val unmappedParameters = parameterNames.filter { name -> sourceProperties.none { it.targetName == name } }

        val ignoredNames = sourceProperties.filter { it.ignore }.map { it.targetName }.toSet()
        val unmappedProperties = (targetData.varProperties.filter { it.hasAccessibleSetter() }.map { it.name }
            + targetData.setter.map { it.name })
            .distinct()
            .filter { it !in parameterNames && it !in writtenNonConstructorNames && it !in ignoredNames }

        if (unmappedParameters.isEmpty() && unmappedProperties.isEmpty()) return

        val targetClass = targetData.classDeclaration
        if (policy == UnmappedTargetPolicy.ERROR) {
            throw UnmappedTargetException(targetClass, unmappedParameters, unmappedProperties)
        }
        unmappedParameters.forEach {
            logger.warn(
                "No source for the target parameter '$it' of ${targetClass.qualifiedName?.asString()}, its default value is used",
                constructor
            )
        }
        unmappedProperties.forEach {
            logger.warn(
                "No source for the target property '$it' of ${targetClass.qualifiedName?.asString()}, its initial value is kept",
                targetClass
            )
        }
    }

    /**
     * A `var` whose setter is private or protected, or internal to another module, cannot be written by the generated code.
     */
    private fun TargetDataExtractionStrategy.TargetVarProperty.hasAccessibleSetter(): Boolean {
        val modifiers = property.setter?.modifiers ?: return true
        return when {
            Modifier.PRIVATE in modifiers || Modifier.PROTECTED in modifiers -> false
            Modifier.INTERNAL in modifiers -> property.origin == Origin.KOTLIN
            else -> true
        }
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
