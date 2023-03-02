package io.mcarle.lib.kmapper.processor.converter.annotated

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.processor.api.TypeConverter
import io.mcarle.lib.kmapper.processor.api.TypeConverterRegistry
import io.mcarle.lib.kmapper.processor.api.isNullable
import org.paukov.combinatorics3.Generator
import kotlin.reflect.KClass

class MapStructureBuilder(
    private val resolver: Resolver,
    private val logger: KSPLogger
) {


    private fun callConstructor(
        constructor: KSFunctionDeclaration,
        sourceProperties: List<Property>,
        indentLevel: Int
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
                null
            } else if (valueParamIsNullable) {
                "null"
            } else {
                null
            }

            if (convertedValue != null) {
                (1..indentLevel).joinToString("") { " " } + if (sourceHasParamNames) {
                    "${sourceProperty.targetName} = $convertedValue"
                } else {
                    convertedValue
                }
            } else {
                null
            }
        }.joinToString(",\n")
    }

    private fun determineSourceProperty(props: List<Property>, ksValueParameter: KSValueParameter): Property {
        return props.firstOrNull {
            it.targetName == ksValueParameter.name?.asString()
        } ?: TODO("handle no matching source property")
    }

    private fun determineSourceProperty(
        props: List<Property>,
        ksPropertyDeclaration: KSPropertyDeclaration
    ): Property {
        return props.firstOrNull {
            it.targetName == ksPropertyDeclaration.simpleName.asString()
        } ?: TODO("handle no matching source property")
    }

    data class Property constructor(
        val mappingParamName: String?,
        val sourceName: String?,
        val targetName: String,
        val constant: String?,
        val expression: String?,
        val ignore: Boolean,
        val enableConverters: List<KClass<out TypeConverter>>,
        val declaration: KSPropertyDeclaration?
    )

    private fun determineProperties(
        mappingParamName: String?,
        mappings: List<KMap>,
        ksClassDeclaration: KSClassDeclaration
    ): List<Property> {
        val sourceMappings = mappings.map { it.source }
        val properties = ksClassDeclaration.getAllProperties().toList()

        verifyAllPropertiesExist(sourceMappings, properties, ksClassDeclaration)

        val result = mappings.filter { it.source.isEmpty() }.map { annotation ->
            Property(
                mappingParamName = mappingParamName,
                sourceName = null,
                targetName = annotation.target,
                constant = annotation.constant.takeIf { it.isNotEmpty() },
                expression = annotation.expression.takeIf { it.isNotEmpty() },
                ignore = annotation.ignore,
                enableConverters = annotation.enable.toList(),
                declaration = null
            )
        }

        return result + properties.map { property ->

            val annotation = mappings.firstOrNull {
                property.simpleName.asString() == it.source
            }

            Property(
                mappingParamName = mappingParamName,
                sourceName = property.simpleName.asString(),
                targetName = annotation?.target ?: property.simpleName.asString(),
                constant = annotation?.constant?.takeIf { it.isNotEmpty() },
                expression = annotation?.expression?.takeIf { it.isNotEmpty() },
                ignore = annotation?.ignore == true,
                enableConverters = annotation?.enable?.toList() ?: emptyList(),
                declaration = property
            )
        }
    }

    private fun verifyAllPropertiesExist(
        sourceMappings: List<String>,
        properties: List<KSPropertyDeclaration>,
        ksClassDeclaration: KSClassDeclaration
    ) {
        sourceMappings.filter { it.isNotEmpty() }.forEach { source ->
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
                if (source.mappingParamName != null) {
                    return "${source.mappingParamName}.let { ${source.expression} }"
                } else {
                    return "let { ${source.expression} }"
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

    private fun determineConstructor(ksClassDeclaration: KSClassDeclaration): KSFunctionDeclaration? {
        val constructors = ksClassDeclaration.getConstructors().toList()
        return if (constructors.size <= 1) {
            constructors.firstOrNull()
        } else {
            constructors.firstOrNull {
                it.parameters.isEmpty()
            }
        }
    }

    private fun findMatchingConstructor(
        ksClassDeclaration: KSClassDeclaration,
        props: List<Property>
    ): KSFunctionDeclaration? {
        val constructors = ksClassDeclaration.getConstructors().toList()
        return constructors.firstOrNull {
            propertiesMatching(
                props,
                determineConstructorParameter(it)
            )
        }
    }

    private fun propertiesMatching(props: List<Property>, parameters: List<KSValueParameter>): Boolean {
        if (props.size <= parameters.size && props.size >= parameters.filter { !it.hasDefault }.size) {
            val combinations = calcCombinations(parameters, props.size)
            return combinations.any { propertiesMatchingExcat(props, it) }
        }
        return false
    }

    private fun propertiesMatchingExcat(props: List<Property>, parameters: List<KSValueParameter>): Boolean {
        if (props.size != parameters.size) return false
        return props.all { prop ->
            parameters.any { param ->
                prop.targetName == param.name!!.asString()
            }
        }
    }

    private fun calcCombinations(parameters: List<KSValueParameter>, parameterCount: Int): List<List<KSValueParameter>> {
        val defaultParameter = parameters.filter { it.hasDefault }
        val nonDefaultParameter = parameters.filter { !it.hasDefault }

        return if (parameterCount == nonDefaultParameter.size) {
            listOf(nonDefaultParameter)
        } else {
            val missingParameter = parameterCount - nonDefaultParameter.size
            Generator
                .combination(defaultParameter)
                .simple(missingParameter)
                .map { nonDefaultParameter + it }
        }
    }

    private fun determineConstructorParameter(constructor: KSFunctionDeclaration): List<KSValueParameter> {
        return constructor.parameters
    }

    private fun KSFunctionDeclaration.isEmptyConstructor() = this.parameters.isEmpty()

    fun rules(mappings: List<KMap>, paramName: String?, source: KSClassDeclaration, target: KSClassDeclaration): String {

        val sourceProperties = determineProperties(paramName, mappings, source).toList()

        var constructor = determineConstructor(target)
        val targetProperties = if (constructor?.isEmptyConstructor() == true) {
            determineMutableProperties(target).map { TargetPropertyOrParam(it) }.also {
                verifyMandatoryPropertiesOrParamsExisting(sourceProperties, it)
            }
        } else {
            if (constructor != null) {
                // non-empty constructor
                val constructorParameters = determineConstructorParameter(constructor)
                val targetPropertiesOrParams = constructorParameters.map { TargetPropertyOrParam(it) }
                verifyMandatoryPropertiesOrParamsExisting(sourceProperties, targetPropertiesOrParams)

                if (propertiesMatching(sourceProperties, constructorParameters)) {
                    // constructor params matching sourceParams
                    targetPropertiesOrParams
                } else {
                    // constructor params not matching sourceParams, combine with mutable properties
                    val targetPropertiesWithParams = targetPropertiesOrParams +
                            determineMutableProperties(target).map { TargetPropertyOrParam(it) }
                    targetPropertiesWithParams.also {
                        verifyMandatoryPropertiesOrParamsExisting(sourceProperties, it)
                    }
                }
            } else {
                constructor = findMatchingConstructor(target, sourceProperties.toList())
                    ?: throw RuntimeException("Could not determine constructor")

                determineConstructorParameter(constructor).map { TargetPropertyOrParam(it) }
            }
        }

        return tryConvert(sourceProperties, constructor, targetProperties)
    }

    private fun tryConvert(
        sourceProperties: List<Property>,
        constructor: KSFunctionDeclaration,
        targetProperties: List<TargetPropertyOrParam>
    ): String {
        if (constructor.isEmptyConstructor()) {
            return """
val gen = ${constructor.parentDeclaration!!.simpleName.asString()}()
${ // @formatter:off
                    setProperties(
                        targetProperties = targetProperties.filter { it.isProperty() }.map { it.getProperty() },
                        sourceProperties = sourceProperties,
                        targetVarName = "gen",
                        indentLevel = 0
                    )
                }
return gen
            """.trimIndent()
                // @formatter:on
        } else {
            if (!containsAdditional(targetProperties, constructor)) {
                return """
return ${constructor.parentDeclaration!!.simpleName.asString()}(
${ // @formatter:off
                        callConstructor(
                            constructor = constructor,
                            sourceProperties = sourceProperties,
                            indentLevel = 4
                        )
                    }
)
                """.trimIndent()
                    // @formatter:on
            } else {
                return """
val gen = ${constructor.parentDeclaration!!.simpleName.asString()}(
${ // @formatter:off
                        callConstructor(
                            constructor = constructor,
                            sourceProperties = sourceProperties,
                            indentLevel = 2
                        )
                    }
)
${ // @formatter:off
                        setProperties(
                            targetProperties = targetProperties.filter { it.isProperty() }.map { it.getProperty() },
                            sourceProperties = sourceProperties,
                            targetVarName = "gen",
                            indentLevel = 0
                        )
                    }
return gen
                """.trimIndent()
                    // @formatter:on
            }
        }
    }

    private fun containsAdditional(
        targetProperties: List<TargetPropertyOrParam>,
        constructor: KSFunctionDeclaration
    ): Boolean {
        val parameters = constructor.parameters
        return targetProperties.filter { it.isProperty() }.map { it.getProperty() }.any { prop ->
            parameters.none { param ->
                param.name?.asString() == prop.simpleName.asString() &&
                        param.type == prop.type
            }
        }
    }

    private fun setProperties(
        targetProperties: List<KSPropertyDeclaration>,
        sourceProperties: List<Property>,
        targetVarName: String,
        indentLevel: Int
    ): String {
        return targetProperties.mapNotNull { targetProperty ->
            val sourceProperty = determineSourceProperty(sourceProperties, targetProperty)
            val convertedValue = convertValue(
                source = sourceProperty,
                targetTypeRef = targetProperty.type,
                ignorable = true
            )
            if (convertedValue != null) {
                (1..indentLevel).joinToString("") { " " } +
                        "$targetVarName.${sourceProperty.targetName} = $convertedValue"
            } else {
                null
            }
        }.joinToString("\n")
    }

    private fun verifyMandatoryPropertiesOrParamsExisting(
        sourceProperties: List<Property>,
        targetPropertiesOrParams: List<TargetPropertyOrParam>
    ) {
        val propertyOrParamWithoutSource = targetPropertiesOrParams.firstOrNull { propertyOrParam ->
            val name = if (propertyOrParam.isProperty()) {
                propertyOrParam.getProperty().simpleName.asString()
            } else {
                if (propertyOrParam.getParameter().hasDefault) return@firstOrNull false // break, as optional
                propertyOrParam.getParameter().name?.asString()
            }
            sourceProperties.none { name == it.targetName }
        }
        if (propertyOrParamWithoutSource != null) {
            throw RuntimeException("Could not determine source property for property/parameter $propertyOrParamWithoutSource")
        }
    }

    data class TargetPropertyOrParam private constructor(
        private val propertyDeclaration: KSPropertyDeclaration? = null,
        private val valueParameter: KSValueParameter? = null
    ) {
        constructor(propertyDeclaration: KSPropertyDeclaration) : this(propertyDeclaration, null)
        constructor(valueParameter: KSValueParameter) : this(null, valueParameter)

        fun isProperty(): Boolean = propertyDeclaration != null
        fun isValue(): Boolean = valueParameter != null

        fun getProperty(): KSPropertyDeclaration {
            return propertyDeclaration!!
        }

        fun getParameter(): KSValueParameter {
            return valueParameter!!
        }
    }

}