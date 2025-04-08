package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.classDeclaration
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

class CodeGenerator constructor(
    private val logger: KSPLogger,
    private val resolver: Resolver
) {

    private val sourceDataExtractionStrategy = DefaultSourceDataExtractionStrategy()
    private val targetDataExtractionStrategy = DefaultTargetDataExtractionStrategy()

    fun generateCode(
        mappings: List<Mapping>,
        enforcedConstructorTypes: List<KSClassDeclaration>,
        paramName: String?,
        targetClassImportName: String?,
        source: KSType,
        target: KSType,
        mappingCodeParentDeclaration: KSDeclaration,
        additionalSourceParameters: List<KSValueParameter>
    ): CodeBlock {
        try {
            if (paramName != null) {
                val existingTypeConverter = TypeConverterRegistry
                    .firstOrNull {
                        it.matches(source, target) && it !is AnnotatedConverter
                    }

                if (existingTypeConverter != null) {
                    return CodeBlock.of(
                        "return·%L",
                        existingTypeConverter.convert(paramName, source, target)
                    )
                }
            }

            val sourceClassDeclaration =
                source.classDeclaration()!!

            val targetClassDeclaration = target.classDeclaration()!!

            val sourceDataList = sourceDataExtractionStrategy.extract(resolver, sourceClassDeclaration, mappingCodeParentDeclaration)
            val targetData = targetDataExtractionStrategy.extract(resolver, targetClassDeclaration, mappingCodeParentDeclaration)

            val sourceProperties = PropertyMappingResolver.determinePropertyMappings(
                mappingParamName = paramName,
                mappings = mappings,
                additionalSourceParameters = additionalSourceParameters,
                sourceDataList = sourceDataList
            )

            val constructor = ConstructorResolver.determineConstructor(
                targetData = targetData,
                sourceProperties = sourceProperties,
                constructorTypes = enforcedConstructorTypes
            )

            val mappingProperties = mappings.map { it.target }.toSet()
            val targetElements = determineTargetElements(mappingProperties, sourceProperties, constructor, targetClassDeclaration)

            verifyPropertiesAndMandatoryParametersExist(sourceProperties, targetElements)

            if (source.isNullable() && !target.isNullable() && !Configuration.enforceNotNull) {
                throw NotNullOperatorNotEnabledException(paramName, source, target)
            }

            val targetPropertiesWithoutParameters = extractDistinctProperties(targetElements)

            return MappingCodeGenerator(logger).generateMappingCode(
                source,
                target,
                sourceProperties.sortedByDescending { it.isBasedOnAnnotation },
                constructor,
                paramName,
                targetClassImportName,
                targetPropertiesWithoutParameters
            )
        } catch (e: Exception) {
            throw KonvertException(source, target, e)
        }
    }

    private fun extractDistinctProperties(targetElements: List<TargetElement>) = targetElements
        .mapNotNull { it.property }
        .filterNot { property ->
            targetElements
                .mapNotNull { it.parameter }
                .any { parameter ->
                    property.simpleName.asString() == parameter.name?.asString() && property.type.resolve() == parameter.type.resolve()
                }
        }

    private fun determineTargetElements(
        mappingProperties: Set<String>,
        sourceProperties: List<PropertyMappingInfo>,
        constructor: KSFunctionDeclaration,
        target: KSClassDeclaration
    ): List<TargetElement> {
        val matchingExact = propertiesMatchingExact(sourceProperties, constructor.parameters)
        val targetElements = if (matchingExact && Configuration.nonConstructorPropertiesMapping == "ignore") {
            // constructor params matching sourceParams
            constructor.parameters
        } else if (constructor.parameters.isEmpty() && Configuration.nonConstructorPropertiesMapping == "strict") {
            logger.warn(
                "Falling back to property-based mapping for class `${target.simpleName.getShortName()}`: " +
                    "target class does not define a primary constructor and 'konvert.non-constructor-properties-mapping=strict'. " +
                    "Using all mutable target properties as mapping candidates.",
                target
            )
            determineMutableProperties(target)
        } else {
            constructor.parameters + determineMutableProperties(target).filter {
                Configuration.nonConstructorPropertiesMapping != "strict" || mappingProperties.contains(it.simpleName.getShortName())
            }
        }
        return targetElements.map { TargetElement(it) }
    }

    private fun verifyPropertiesAndMandatoryParametersExist(
        propertyMappings: List<PropertyMappingInfo>,
        targetElements: List<TargetElement>
    ) {
        val targetElement = targetElements.firstOrNull { targetElement ->
            val name = if (targetElement.property != null) {
                targetElement.property.simpleName.asString()
            } else if (targetElement.parameter != null) {
                if (targetElement.parameter.hasDefault)
                    return@firstOrNull false // break, as optional
                else if (targetElement.parameter.type.resolve().isNullable())
                    return@firstOrNull false // break, as nullable
                targetElement.parameter.name?.asString()
            } else {
                // should not occur...
                null
            }
            propertyMappings.none { name == it.targetName }
        }
        if (!Configuration.ignoreUnmappedTargetProperties && targetElement != null) {
            throw PropertyMappingNotExistingException(targetElement, propertyMappings)
        }
    }

    @OptIn(KspExperimental::class)
    private fun determineMutableProperties(ksClassDeclaration: KSClassDeclaration): List<KSPropertyDeclaration> {
        return ksClassDeclaration.getAllProperties()
            .filter { it.extensionReceiver == null }
            .filter { it.isMutable }
            .filter { !it.isAnnotationPresent(Transient::class) } // CHECKME: is it correct to exclude transient?
            .toList()
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

    class TargetElement private constructor(
        val property: KSPropertyDeclaration? = null,
        val parameter: KSValueParameter? = null
    ) {
        constructor(annotated: KSAnnotated) : this(annotated as? KSPropertyDeclaration, annotated as? KSValueParameter)

        override fun toString(): String {
            return when {
                property != null -> property.simpleName.asString()
                parameter != null -> parameter.name?.asString() ?: parameter.toString()
                else -> error("No property or parameter available")
            }
        }

    }

}
