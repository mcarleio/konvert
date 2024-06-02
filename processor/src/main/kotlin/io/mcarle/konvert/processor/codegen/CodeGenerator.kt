package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.enforceNotNull
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.DefaultSourceDataExtractionStrategy
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException

data class Source(
    val paramName: String?,
    val type: KSType
)

class CodeGenerator constructor(
    private val logger: KSPLogger,
    private val resolver: Resolver
) {


    fun generateCode(
        mappings: List<Mapping>,
        constructorTypes: List<KSClassDeclaration>,
        targetClassImportName: String?,
        sources: List<Source>,
        target: KSType,
        mappingCodeParentDeclaration: KSDeclaration,
        additionalSourceParameters: List<KSValueParameter>
    ): CodeBlock {
        sources.forEach { source ->
            if (source.paramName != null) {
                val existingTypeConverter = TypeConverterRegistry
                    .firstOrNull {
                        it.matches(source.type, target) && it !is AnnotatedConverter
                    }

                if (existingTypeConverter != null) {
                    return CodeBlock.of(
                        "return·%L",
                        existingTypeConverter.convert(source.paramName, source.type, target)
                    )
                }
            }
        }
        val propertyResolver = PropertyMappingResolver(
            logger,
            DefaultSourceDataExtractionStrategy(mappingCodeParentDeclaration, resolver.builtIns.unitType)
        )

        val sourceProperties = sources.flatMap { source ->
            propertyResolver.determinePropertyMappings(
                source,
                mappings,
                additionalSourceParameters
            )
        }

        val targetClassDeclaration = target.classDeclaration()!!

        val constructor = ConstructorResolver(logger)
            .determineConstructor(mappingCodeParentDeclaration, targetClassDeclaration, sourceProperties, constructorTypes)

        val targetElements = determineTargetElements(sourceProperties, constructor, targetClassDeclaration)

        verifyPropertiesAndMandatoryParametersExist(sourceProperties, targetElements)
        verifyNotNullOperatorNotNeededOrEnabled(sources, target)

        val targetPropertiesWithoutParameters = extractDistinctProperties(targetElements)

        return MappingCodeGenerator().generateMappingCode(
            sources,
            target,
            sourceProperties.sortedByDescending { it.isBasedOnAnnotation },
            constructor,
            targetClassImportName,
            targetPropertiesWithoutParameters
        )
    }

    private fun verifyNotNullOperatorNotNeededOrEnabled(sources: List<Source>, target: KSType) {
        if (!target.isNullable() && !Configuration.enforceNotNull) {
            sources.firstOrNull { it.type.isNullable() }?.let {
                throw NotNullOperatorNotEnabledException(it.paramName, it.type, target)
            }
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
        sourceProperties: List<PropertyMappingInfo>,
        constructor: KSFunctionDeclaration,
        target: KSClassDeclaration
    ) = if (propertiesMatchingExact(sourceProperties, constructor.parameters)) {
        // constructor params matching sourceParams
        constructor.parameters
    } else {
        // constructor params not matching sourceParams, combine with mutable properties
        constructor.parameters + determineMutableProperties(target)
    }.map { TargetElement(it) }

    private fun verifyPropertiesAndMandatoryParametersExist(
        propertyMappings: List<PropertyMappingInfo>,
        targetElements: List<TargetElement>
    ) {
        val targetElement = targetElements.firstOrNull { targetElement ->
            val name = if (targetElement.property != null) {
                targetElement.property.simpleName.asString()
            } else if (targetElement.parameter != null) {
                if (targetElement.parameter.hasDefault) return@firstOrNull false // break, as optional
                targetElement.parameter.name?.asString()
            } else {
                // should not occur...
                null
            }
            propertyMappings.none { name == it.targetName }
        }
        if (targetElement != null) {
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
        val parameter: KSValueParameter? = null
    ) {
        constructor(annotated: KSAnnotated) : this(annotated as? KSPropertyDeclaration, annotated as? KSValueParameter)

        override fun toString(): String {
            return if (property != null) {
                "propertyDeclaration=$property"
            } else {
                "valueParameter=$parameter"
            }
        }

    }

}
