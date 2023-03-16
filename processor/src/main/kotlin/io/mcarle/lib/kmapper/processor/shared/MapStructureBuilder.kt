package io.mcarle.lib.kmapper.processor.shared

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import io.mcarle.lib.kmapper.converter.api.TypeConverterRegistry
import io.mcarle.lib.kmapper.converter.api.isNullable
import java.util.*
import kotlin.reflect.KClass

class MapStructureBuilder(
    private val logger: KSPLogger
) {

    fun generateCode(
        mappings: List<KMap>,
        constructorTypes: List<KSClassDeclaration>,
        paramName: String?,
        source: KSClassDeclaration,
        target: KSClassDeclaration,
        mappingCodeParentDeclaration: KSDeclaration
    ): String {

        val sourceProperties = determineProperties(paramName, mappings, source)

        val constructor = determineConstructor(mappingCodeParentDeclaration, target, sourceProperties, constructorTypes)

        val targetProperties = if (propertiesMatchingExact(sourceProperties, constructor.parameters)) {
            // constructor params matching sourceParams
            constructor.parameters
        } else {
            // constructor params not matching sourceParams, combine with mutable properties
            constructor.parameters + determineMutableProperties(target)
        }.map { TargetPropertyOrParam(it) }

        verifyPropertiesAndMandatoryParamsExisting(sourceProperties, targetProperties)

        val targetPropertiesWithoutParameters = targetProperties
            .mapNotNull { it.property }
            .filterNot { property ->
                targetProperties
                    .mapNotNull { it.parameter }
                    .any { parameter ->
                        property.simpleName.asString() == parameter.name?.asString() && property.type.resolve() == parameter.type.resolve()
                    }
            }

        return convertCode(sourceProperties.sortedByDescending { it.isBasedOnAnnotation }, constructor, targetPropertiesWithoutParameters)
    }

    private fun determineSourceProperty(
        props: List<Property>,
        ksValueParameter: KSValueParameter
    ): Property {
        return props.firstOrNull {
            it.targetName == ksValueParameter.name?.asString()
        } ?: throw UnexpectedStateException("No property for $ksValueParameter existing in $props")
    }

    private fun determineSourceProperty(
        props: List<Property>,
        ksPropertyDeclaration: KSPropertyDeclaration
    ): Property {
        return props.firstOrNull {
            it.targetName == ksPropertyDeclaration.simpleName.asString()
        } ?: throw UnexpectedStateException("No property for $ksPropertyDeclaration existing in $props")
    }

    private fun determineProperties(
        mappingParamName: String?,
        mappings: List<KMap>,
        ksClassDeclaration: KSClassDeclaration
    ): List<Property> {
        val properties = ksClassDeclaration.getAllProperties().toList()

        verifyAllPropertiesExist(mappings, properties, ksClassDeclaration)

        val propertiesWithoutSource = getPropertiesWithoutSource(mappings, mappingParamName)
        val propertiesWithSource = getPropertiesWithSource(mappings, properties, mappingParamName)
        val propertiesWithoutMappings = getPropertiesWithoutMappings(properties, mappingParamName)

        return propertiesWithoutSource + propertiesWithSource + propertiesWithoutMappings
    }

    private fun getPropertiesWithoutMappings(
        properties: List<KSPropertyDeclaration>,
        mappingParamName: String?
    ) = properties
        .map { property ->
            Property(
                mappingParamName = mappingParamName,
                sourceName = property.simpleName.asString(),
                targetName = property.simpleName.asString(),
                constant = null,
                expression = null,
                ignore = false,
                enableConverters = emptyList(),
                declaration = property,
                isBasedOnAnnotation = false
            )
        }

    private fun getPropertiesWithSource(
        mappings: List<KMap>,
        properties: List<KSPropertyDeclaration>,
        mappingParamName: String?
    ) = mappings.filter { it.source.isNotEmpty() }.mapNotNull { annotation ->
        properties.firstOrNull { property ->
            property.simpleName.asString() == annotation.source
        }?.let { annotation to it }
    }.map { (annotation, property) ->
        Property(
            mappingParamName = mappingParamName,
            sourceName = property.simpleName.asString(),
            targetName = annotation.target,
            constant = annotation.constant.takeIf { it.isNotEmpty() },
            expression = annotation.expression.takeIf { it.isNotEmpty() },
            ignore = annotation.ignore,
            enableConverters = annotation.enable.toList(),
            declaration = property,
            isBasedOnAnnotation = true
        )
    }

    private fun getPropertiesWithoutSource(
        mappings: List<KMap>,
        mappingParamName: String?
    ) = mappings.filter { it.source.isEmpty() }.map { annotation ->
        Property(
            mappingParamName = mappingParamName,
            sourceName = null,
            targetName = annotation.target,
            constant = annotation.constant.takeIf { it.isNotEmpty() },
            expression = annotation.expression.takeIf { it.isNotEmpty() },
            ignore = annotation.ignore,
            enableConverters = annotation.enable.toList(),
            declaration = null,
            isBasedOnAnnotation = true
        )
    }

    private fun verifyAllPropertiesExist(
        mappings: List<KMap>,
        properties: List<KSPropertyDeclaration>,
        ksClassDeclaration: KSClassDeclaration
    ) {
        mappings.map { it.source }.filter { it.isNotEmpty() }.forEach { source ->
            if (properties.none { it.simpleName.asString() == source }) {
                logger.warn("Ignoring mapping: $source not existing in ${ksClassDeclaration.simpleName.asString()}")
            }
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

    private fun convertValue(source: Property, targetTypeRef: KSTypeReference, ignorable: Boolean): String? {
        val targetType = targetTypeRef.resolve()

        if (source.declaration == null) {
            if (source.ignore && ignorable) {
                return null
            }
            if (source.constant != null) {
                return source.constant
            }
            if (source.expression != null) {
                return if (source.mappingParamName != null) {
                    "${source.mappingParamName}.let { ${source.expression} }"
                } else {
                    "let { ${source.expression} }"
                }
            }
            throw IllegalStateException("Could not convert value $source")
        } else {
            val sourceType = source.declaration.type.resolve()

            val paramName = source.mappingParamName?.let { "$it." } ?: ""

            return TypeConverterRegistry.withAdditionallyEnabledConverters(source.enableConverters) {
                firstOrNull { it.matches(sourceType, targetType) }
                    ?.convert(paramName + source.sourceName!!, sourceType, targetType)
                    ?: throw NoSuchElementException("Could not find converter for ${paramName + source.sourceName} -> ${source.targetName}: $sourceType -> $targetType")
            }

        }
    }

    private fun determineConstructor(
        mappingCodeParentDeclaration: KSDeclaration,
        targetClassDeclaration: KSClassDeclaration,
        sourceProperties: List<Property>,
        constructorTypes: List<KSClassDeclaration>
    ): KSFunctionDeclaration {
        val visibleConstructors = targetClassDeclaration.getConstructors()
            .filter { it.isVisibleFrom(mappingCodeParentDeclaration) }.toList()

        return if (constructorTypes.firstOrNull()?.qualifiedName?.asString() == Unit::class.qualifiedName) {
            if (targetClassDeclaration.primaryConstructor != null
                && targetClassDeclaration.primaryConstructor!!.isVisibleFrom(mappingCodeParentDeclaration)
                && propertiesMatching(
                    sourceProperties,
                    targetClassDeclaration.primaryConstructor!!.parameters
                )
            ) {
                // Primary constructor
                targetClassDeclaration.primaryConstructor!!
            } else {
                determineSingleOrEmptyConstructor(visibleConstructors)
                    ?: findMatchingConstructors(visibleConstructors, sourceProperties)
                        .let {
                            if (it.size > 1) {
                                throw AmbiguousConstructorException(targetClassDeclaration, it)
                            } else if (it.isEmpty()) {
                                throw NoMatchingConstructorException(targetClassDeclaration, *sourceProperties.toTypedArray())
                            } else {
                                it.first()
                            }
                        }
            }
        } else {
            findConstructorByParameterTypes(visibleConstructors, constructorTypes)
                ?: throw NoMatchingConstructorException(targetClassDeclaration, *constructorTypes.toTypedArray())
        }
    }

    private fun findConstructorByParameterTypes(
        constructors: List<KSFunctionDeclaration>,
        constructorTypes: List<KSClassDeclaration>
    ): KSFunctionDeclaration? {
        return constructors.firstOrNull { constructor ->
            constructor.parameters.mapNotNull { it.typeClassDeclaration() } == constructorTypes
        }
    }

    private fun determineSingleOrEmptyConstructor(constructors: List<KSFunctionDeclaration>): KSFunctionDeclaration? {
        return if (constructors.size <= 1) {
            constructors.firstOrNull()
        } else {
            constructors.firstOrNull {
                it.parameters.isEmpty()
            }
        }
    }

    private fun findMatchingConstructors(
        constructors: List<KSFunctionDeclaration>,
        props: List<Property>
    ): List<KSFunctionDeclaration> {
        return constructors
            .filter {
                propertiesMatching(
                    props,
                    it.parameters
                )
            }
    }

    private fun propertiesMatching(props: List<Property>, parameters: List<KSValueParameter>): Boolean {
        if (props.size >= parameters.filter { !it.hasDefault }.size) {
            return parameters.all { parameter ->
                props.any { property ->
                    property.targetName == parameter.name?.asString() && !property.ignore
                }
            }
        }
        return false
    }

    private fun propertiesMatchingExact(props: List<Property>, parameters: List<KSValueParameter>): Boolean {
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

    private fun convertCode(
        sourceProperties: List<Property>,
        constructor: KSFunctionDeclaration,
        targetProperties: List<KSPropertyDeclaration>
    ): String {
        val className = constructor.parentDeclaration!!.simpleName.asString()
        val constructorCode = constructorCode(className, constructor, sourceProperties)
        return "return·" + constructorCode + propertyCode(className, sourceProperties, targetProperties)
    }

    private fun constructorCode(
        className: String,
        constructor: KSFunctionDeclaration,
        sourceProperties: List<Property>
    ): String {
        return if (constructor.parameters.isEmpty()) {
            "$className()"
        } else {
            """
$className(${"⇥\n" + constructorParamsCode(constructor = constructor, sourceProperties = sourceProperties)}
⇤)
            """.trimIndent()
        }
    }

    private fun constructorParamsCode(
        constructor: KSFunctionDeclaration,
        sourceProperties: List<Property>
    ): String {
        return constructor.parameters.mapNotNull { ksValueParameter ->
            val sourceHasParamNames = constructor.origin !in listOf(
                Origin.JAVA,
                Origin.JAVA_LIB
            )
            val valueParamHasDefault = ksValueParameter.hasDefault && sourceHasParamNames
            val valueParamIsNullable = ksValueParameter.type.resolve().isNullable()

            val sourceProperty = determineSourceProperty(sourceProperties, ksValueParameter)
            val convertedValue = convertValue(
                source = sourceProperty,
                targetTypeRef = ksValueParameter.type,
                ignorable = valueParamHasDefault || valueParamIsNullable
            ) ?: if (valueParamHasDefault) {
                // when constructor param has a default value, ignore it
                null
            } else if (valueParamIsNullable) {
                // when constructor param is nullable, set it to null
                "null"
            } else {
                null
            }

            if (convertedValue != null) {
                if (sourceHasParamNames) {
                    "${sourceProperty.targetName}·=·$convertedValue"
                } else {
                    convertedValue
                }
            } else {
                null
            }
        }.joinToString(separator = ",\n")
    }

    private fun propertyCode(
        className: String,
        sourceProperties: List<Property>,
        targetProperties: List<KSPropertyDeclaration>
    ): String {
        if (targetProperties.isEmpty()) return ""
        val varName = className.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        return """
.also { $varName ->${"⇥\n" + propertySettingCode(targetProperties, sourceProperties, varName)}
⇤}
        """.trimIndent()
    }

    private fun propertySettingCode(
        targetProperties: List<KSPropertyDeclaration>,
        sourceProperties: List<Property>,
        targetVarName: String
    ): String {
        return targetProperties.mapNotNull { targetProperty ->
            val sourceProperty = determineSourceProperty(sourceProperties, targetProperty)
            val convertedValue = convertValue(
                source = sourceProperty,
                targetTypeRef = targetProperty.type,
                ignorable = true
            )
            if (convertedValue != null) {
                "$targetVarName.${sourceProperty.targetName}·=·$convertedValue"
            } else {
                null
            }
        }.joinToString("\n")
    }

    private fun verifyPropertiesAndMandatoryParamsExisting(
        sourceProperties: List<Property>,
        targetPropertiesOrParams: List<TargetPropertyOrParam>
    ) {
        val propertyOrParamWithoutSource = targetPropertiesOrParams.firstOrNull { propertyOrParam ->
            val name = if (propertyOrParam.property != null) {
                propertyOrParam.property.simpleName.asString()
            } else if (propertyOrParam.parameter != null) {
                if (propertyOrParam.parameter.hasDefault) return@firstOrNull false // break, as optional
                propertyOrParam.parameter.name?.asString()
            } else {
                // should not occur...
                null
            }
            sourceProperties.none { name == it.targetName }
        }
        if (propertyOrParamWithoutSource != null) {
            throw RuntimeException("Could not determine source property for property/parameter $propertyOrParamWithoutSource")
        }
    }

    data class Property constructor(
        val mappingParamName: String?,
        val sourceName: String?,
        val targetName: String,
        val constant: String?,
        val expression: String?,
        val ignore: Boolean,
        val enableConverters: List<KClass<out TypeConverter>>,
        val declaration: KSPropertyDeclaration?,
        val isBasedOnAnnotation: Boolean
    )

    class TargetPropertyOrParam private constructor(
        val property: KSPropertyDeclaration? = null,
        val parameter: KSValueParameter? = null
    ) {
        constructor(propertyDeclaration: KSPropertyDeclaration) : this(propertyDeclaration, null)
        constructor(valueParameter: KSValueParameter) : this(null, valueParameter)
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