package io.mcarle.lib.kmapper.processor.shared

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import io.mcarle.lib.kmapper.converter.api.TypeConverterRegistry
import io.mcarle.lib.kmapper.converter.api.isNullable
import org.paukov.combinatorics3.Generator
import kotlin.reflect.KClass

class MapStructureBuilder(
    private val logger: KSPLogger
) {

    fun generateCode(mappings: List<KMap>, paramName: String?, source: KSClassDeclaration, target: KSClassDeclaration): String {

        val sourceProperties = determineProperties(paramName, mappings, source).toList()

        val constructor = determineSingleOrEmptyConstructor(target)
            ?: findMatchingConstructor(target, sourceProperties.toList())
            ?: throw RuntimeException("Could not determine constructor")

        val targetProperties = if (constructor.isEmptyConstructor()) {
            determineMutableProperties(target).map { TargetPropertyOrParam(it) }
        } else {
            // non-empty constructor
            val constructorParameters = constructor.parameters
            val targetPropertiesOrParams = constructorParameters.map { TargetPropertyOrParam(it) }

            if (propertiesMatching(sourceProperties, constructorParameters)) {
                // constructor params matching sourceParams
                targetPropertiesOrParams
            } else {
                // constructor params not matching sourceParams, combine with mutable properties
                val targetPropertiesWithParams = targetPropertiesOrParams +
                        determineMutableProperties(target).map { TargetPropertyOrParam(it) }
                targetPropertiesWithParams
            }
        }

        verifyPropertiesAndMandatoryParamsExisting(sourceProperties, targetProperties)

        val targetPropertiesWithoutParameters = targetProperties
            .mapNotNull { it.property }
            .filterNot { property ->
                targetProperties
                    .mapNotNull { it.parameter }
                    .any { parameter ->
                        property.simpleName.asString() == parameter.name?.asString() && property.type == parameter.type
                    }
            }

        return tryConvert(sourceProperties, constructor, targetPropertiesWithoutParameters)
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

    private fun determineSingleOrEmptyConstructor(ksClassDeclaration: KSClassDeclaration): KSFunctionDeclaration? {
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
        return constructors
            .filter {
                propertiesMatching(
                    props,
                    it.parameters
                )
            }
            .maxByOrNull { it.parameters.size } // TODO: is it always good to choose constructor with the most parameters?
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

    private fun KSFunctionDeclaration.isEmptyConstructor() = this.parameters.isEmpty()

    private fun tryConvert(
        sourceProperties: List<Property>,
        constructor: KSFunctionDeclaration,
        targetProperties: List<KSPropertyDeclaration>
    ): String {
        val constructorCode = constructorCode(constructor, sourceProperties)
        return "return " + constructorCode + propertyCode(sourceProperties, targetProperties)
//        return "return " + if (containsAdditional(targetProperties, constructor)) {
//            constructorCode + propertyCode(sourceProperties, targetProperties)
//        } else {
//            constructorCode
//        }
    }

    private fun constructorCode(
        constructor: KSFunctionDeclaration,
        sourceProperties: List<Property>
    ): String {
        val className = constructor.parentDeclaration!!.simpleName.asString()
        return if (constructor.parameters.isEmpty()) {
            "$className()"
        } else {
            """
«$className(${"\n" + constructorParamsCode(constructor = constructor, sourceProperties = sourceProperties)}
»)
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

//    private fun propertyCode(
//        sourceProperties: List<Property>,
//        targetProperties: List<TargetPropertyOrParam>
//    ): String {
//        return propertyCode(sourceProperties, targetProperties.mapNotNull { it.property })
//    }

    private fun propertyCode(
        sourceProperties: List<Property>,
        targetProperties: List<KSPropertyDeclaration>
    ): String {
        if (targetProperties.isEmpty()) return ""
        val varName = "gen"
        return """
«.also { $varName ->${"\n" + propertySettingCode(targetProperties, sourceProperties, varName)}
»}
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

//    private fun someArgsConstructorCode(
//        constructor: KSFunctionDeclaration,
//        sourceProperties: List<Property>,
//        targetProperties: List<TargetPropertyOrParam>
//    ): String {
//        return """
//val gen = ${constructor.parentDeclaration!!.simpleName.asString()}(
//${ // @formatter:off
//        constructorParamsCode(
//            constructor = constructor,
//            sourceProperties = sourceProperties,
//            indentLevel = 2
//        )
//    }
//)
//${ // @formatter:off
//        setProperties(
//            targetProperties = targetProperties.filter { it.isProperty() }.map { it.getProperty() },
//            sourceProperties = sourceProperties,
//            targetVarName = "gen",
//            indentLevel = 0
//        )
//    }
//return gen
//        """.trimIndent()
//        // @formatter:on
//    }
//
//    private fun allArgsConstructorCode(
//        constructor: KSFunctionDeclaration,
//        sourceProperties: List<Property>
//    ): String {
//        return """
//return ${constructor.parentDeclaration!!.simpleName.asString()}(
//${ // @formatter:off
//        constructorParamsCode(
//            constructor = constructor,
//            sourceProperties = sourceProperties,
//            indentLevel = 4
//        )
//    }
//)
//        """.trimIndent()
//        // @formatter:on
//    }
//
//    private fun noArgsConstructorCode(
//        constructor: KSFunctionDeclaration,
//        targetProperties: List<TargetPropertyOrParam>,
//        sourceProperties: List<Property>
//    ): String {
//        return """
//val gen = ${constructor.parentDeclaration!!.simpleName.asString()}()
//${ // @formatter:off
//        setProperties(
//            targetProperties = targetProperties.filter { it.isProperty() }.map { it.getProperty() },
//            sourceProperties = sourceProperties,
//            targetVarName = "gen",
//            indentLevel = 0
//        )
//    }
//return gen
//        """.trimIndent()
//        // @formatter:on
//    }

    private fun containsAdditional(
        targetProperties: List<KSPropertyDeclaration>,
        constructor: KSFunctionDeclaration
    ): Boolean {
        val parameters = constructor.parameters
        return targetProperties.any { prop ->
            parameters.none { param -> param.name?.asString() == prop.simpleName.asString() && param.type == prop.type }
        }
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

    class TargetPropertyOrParam private constructor(
        val property: KSPropertyDeclaration? = null,
        val parameter: KSValueParameter? = null
    ) {
        constructor(propertyDeclaration: KSPropertyDeclaration) : this(propertyDeclaration, null)
        constructor(valueParameter: KSValueParameter) : this(null, valueParameter)

        fun isSame(other: TargetPropertyOrParam): Boolean {
            val (thisName, thisType) = if (property != null) {
                property.simpleName.asString() to property.type
            } else {
                parameter!!.name?.asString() to parameter.type
            }
            val (otherName, otherType) = if (other.property != null) {
                other.property.simpleName.asString() to other.property.type
            } else {
                other.parameter!!.name?.asString() to other.parameter.type
            }
            return thisName == otherName && thisType == otherType
        }

        override fun toString(): String {
            return if (property != null) {
                "propertyDeclaration=$property"
            } else {
                "valueParameter=$parameter"
            }
        }

    }

}