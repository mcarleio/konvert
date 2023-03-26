package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Origin
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import java.util.Locale

class MappingCodeGenerator {

    fun generateMappingCode(
        sourceProperties: List<PropertyMappingInfo>,
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
        sourceProperties: List<PropertyMappingInfo>
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
        sourceProperties: List<PropertyMappingInfo>
    ): String {
        return constructor.parameters.mapNotNull { ksValueParameter ->
            val sourceHasParamNames = constructor.origin !in listOf(
                Origin.JAVA,
                Origin.JAVA_LIB
            )
            val valueParamHasDefault = ksValueParameter.hasDefault && sourceHasParamNames
            val valueParamIsNullable = ksValueParameter.type.resolve().isNullable()

            val sourcePropertyMappingInfo = determinePropertyMappingInfo(sourceProperties, ksValueParameter)
            val convertedValue = convertValue(
                source = sourcePropertyMappingInfo,
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
                    "${sourcePropertyMappingInfo.targetName}·=·$convertedValue"
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
        sourceProperties: List<PropertyMappingInfo>,
        targetProperties: List<KSPropertyDeclaration>
    ): String {
        if (noTargetOrAllIgnored(sourceProperties, targetProperties)) return ""
        val varName = className.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        return """
.also { $varName ->${"⇥\n" + propertySettingCode(targetProperties, sourceProperties, varName)}
⇤}
        """.trimIndent()
    }

    private fun noTargetOrAllIgnored(sourceProperties: List<PropertyMappingInfo>, targetProperties: List<KSPropertyDeclaration>): Boolean {
        return targetProperties.all { targetProperty ->
            sourceProperties.any { sourceProperty ->
                sourceProperty.ignore
                    && sourceProperty.targetName == targetProperty.simpleName.asString()
            }
        }
    }

    private fun propertySettingCode(
        targetProperties: List<KSPropertyDeclaration>,
        sourceProperties: List<PropertyMappingInfo>,
        targetVarName: String
    ): String {
        return targetProperties.mapNotNull { targetProperty ->
            val sourceProperty = determinePropertyMappingInfo(sourceProperties, targetProperty)
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

    private fun determinePropertyMappingInfo(
        propertyMappings: List<PropertyMappingInfo>,
        ksValueParameter: KSValueParameter
    ): PropertyMappingInfo {
        return propertyMappings.firstOrNull {
            it.targetName == ksValueParameter.name?.asString()
        } ?: throw PropertyMappingNotExistingException(ksValueParameter, propertyMappings)
    }

    private fun determinePropertyMappingInfo(
        propertyMappings: List<PropertyMappingInfo>,
        ksPropertyDeclaration: KSPropertyDeclaration
    ): PropertyMappingInfo {
        return propertyMappings.firstOrNull {
            it.targetName == ksPropertyDeclaration.simpleName.asString()
        } ?: throw PropertyMappingNotExistingException(ksPropertyDeclaration, propertyMappings)
    }

    private fun convertValue(source: PropertyMappingInfo, targetTypeRef: KSTypeReference, ignorable: Boolean): String? {
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
}
