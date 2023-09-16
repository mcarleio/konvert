package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
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
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException

class CodeGenerator(
    private val logger: KSPLogger
) {

    fun generateCode(
        mappings: List<Mapping>,
        constructorTypes: List<KSClassDeclaration>,
        paramName: String?,
        targetClassImportName: String?,
        source: KSType,
        target: KSType,
        mappingCodeParentDeclaration: KSDeclaration
    ): CodeBlock {
        if (paramName != null) {
            val existingTypeConverter = TypeConverterRegistry
                .firstOrNull {
                    it.matches(source, target) && it !is AnnotatedConverter
                }

            if (existingTypeConverter != null) {
                logger.logging("", source.declaration)
                return CodeBlock.of(
                    "return·%L",
                    existingTypeConverter.convert(paramName, source, target)
                )
            }
        }

        val sourceProperties = PropertyMappingResolver(logger).determinePropertyMappings(paramName, mappings, source)

        val targetClassDeclaration = target.classDeclaration()!!

        val constructor = ConstructorResolver(logger)
            .determineConstructor(mappingCodeParentDeclaration, targetClassDeclaration, sourceProperties, constructorTypes)

        val targetElements = determineTargetElements(sourceProperties, constructor, targetClassDeclaration)

        verifyPropertiesAndMandatoryParametersExist(sourceProperties, targetElements)

        if (source.isNullable() && !target.isNullable() && !Configuration.enforceNotNull) {
            throw NotNullOperatorNotEnabledException(source, target)
        }

        val targetPropertiesWithoutParameters = extractDistinctProperties(targetElements)

        return MappingCodeGenerator().generateMappingCode(
            source,
            target,
            sourceProperties.sortedByDescending { it.isBasedOnAnnotation },
            constructor,
            paramName,
            targetClassImportName,
            targetPropertiesWithoutParameters
        )
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
