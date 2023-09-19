package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.enableConverters
import io.mcarle.konvert.converter.api.config.enforceNotNull
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import java.util.Locale

class MappingCodeGenerator {

    fun generateMappingCode(
        source: KSType,
        target: KSType,
        sourceProperties: List<PropertyMappingInfo>,
        constructor: KSFunctionDeclaration,
        functionParamName: String?,
        targetClassImportName: String?,
        targetProperties: List<KSPropertyDeclaration>
    ): CodeBlock {
        val typeName = targetClassImportName ?: constructor.parentDeclaration?.qualifiedName!!.asString()
        val className = constructor.parentDeclaration!!.simpleName.asString()
        val constructorCode = constructorCode(typeName, constructor, sourceProperties)
        val propertyCode = propertyCode(
            className,
            functionParamName,
            sourceProperties,
            targetProperties
        )
        return if (source.isNullable()) {
            // source can only be nullable in case of @Konverter/@Konvert
            assert(functionParamName != null)
            val code = "return·$functionParamName?.let·{\n⇥%L%L⇤\n}"
            if (target.isNullable()) {
                CodeBlock.of(code, constructorCode, propertyCode)
            } else {
                if (Configuration.enforceNotNull) {
                    CodeBlock.of("$code!!", constructorCode, propertyCode)
                } else {
                    throw NotNullOperatorNotEnabledException(source, target)
                }
            }
        } else {
            CodeBlock.of("return·%L%L", constructorCode, propertyCode)
        }
    }

    private fun constructorCode(
        className: String,
        constructor: KSFunctionDeclaration,
        sourceProperties: List<PropertyMappingInfo>
    ): CodeBlock {
        return if (constructor.parameters.isEmpty()) {
            CodeBlock.of("$className()")
        } else {
            CodeBlock.of(
                """
$className(${"⇥\n%L"}
⇤)
            """.trimIndent(), constructorParamsCode(constructor = constructor, sourceProperties = sourceProperties)
            )
        }
    }

    private fun constructorParamsCode(
        constructor: KSFunctionDeclaration,
        sourceProperties: List<PropertyMappingInfo>
    ): CodeBlock {
        return constructor.parameters.mapNotNull { ksValueParameter ->
            val sourceHasParamNames = constructor.origin !in listOf(
                Origin.JAVA,
                Origin.JAVA_LIB
            )
            val valueParamHasDefault = ksValueParameter.hasDefault && sourceHasParamNames
            val valueParamIsNullable = ksValueParameter.type.resolve().isNullable()

            val sourcePropertyMappingInfo = try {
                determinePropertyMappingInfo(sourceProperties, ksValueParameter)
            } catch (e: PropertyMappingNotExistingException) {
                if (valueParamHasDefault) {
                    return@mapNotNull null
                } else {
                    throw e
                }
            }
            val convertedValue = convertValue(
                source = sourcePropertyMappingInfo,
                targetTypeRef = ksValueParameter.type,
                ignorable = valueParamHasDefault || valueParamIsNullable
            ) ?: if (valueParamHasDefault) {
                // when constructor param has a default value, ignore it
                null
            } else if (valueParamIsNullable) {
                // when constructor param is nullable, set it to null
                CodeBlock.of("null")
            } else {
                null
            }

            if (convertedValue != null) {
                if (sourceHasParamNames) {
                    CodeBlock.of("${sourcePropertyMappingInfo.targetName}·=·%L", convertedValue)
                } else {
                    convertedValue
                }
            } else {
                null
            }
        }.joinToCode(separator = ",\n")
    }

    private fun propertyCode(
        className: String,
        functionParamName: String?,
        sourceProperties: List<PropertyMappingInfo>,
        targetProperties: List<KSPropertyDeclaration>
    ): CodeBlock {
        if (noTargetOrAllIgnored(sourceProperties, targetProperties)) return CodeBlock.of("")

        var varName = className.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        if (varName == functionParamName) {
            varName += "0"
        }

        return CodeBlock.of(
            """
.also·{·$varName·->${"⇥\n%L"}
⇤}
        """.trimIndent(), propertySettingCode(targetProperties, sourceProperties, varName)
        )
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
    ): CodeBlock {
        return targetProperties.mapNotNull { targetProperty ->
            val sourceProperty = determinePropertyMappingInfo(sourceProperties, targetProperty)
            val convertedValue = convertValue(
                source = sourceProperty,
                targetTypeRef = targetProperty.type,
                ignorable = true
            )
            if (convertedValue != null) {
                CodeBlock.of("$targetVarName.${sourceProperty.targetName}·=·$convertedValue")
            } else {
                null
            }
        }.joinToCode("\n")
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

    private fun convertValue(source: PropertyMappingInfo, targetTypeRef: KSTypeReference, ignorable: Boolean): CodeBlock? {
        val targetType = targetTypeRef.resolve()

        if (source.declaration == null) {
            if (source.ignore && ignorable) {
                return null
            }
            if (source.constant != null) {
                return CodeBlock.of(source.constant)
            }
            if (source.expression != null) {
                return CodeBlock.of(
                    if (source.mappingParamName != null) {
                        "${source.mappingParamName}.let·{ ${source.expression} }"
                    } else {
                        "let·{ ${source.expression} }"
                    }
                )
            }
            throw IllegalStateException("Could not convert value $source")
        } else {
            val sourceType = source.declaration.type.resolve()
            val paramName = source.mappingParamName?.let { "$it." } ?: ""

            return TypeConverterRegistry.withAdditionallyEnabledConverters(source.enableConverters + Configuration.enableConverters) {
                firstOrNull { it.matches(sourceType, targetType) }
                    ?.convert(paramName + source.sourceName!!, sourceType, targetType)
                    ?: throw NoSuchElementException("Could not find converter for ${paramName + source.sourceName} -> ${source.targetName}: $sourceType -> $targetType")
            }

        }
    }
}
