package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.enableConverters
import io.mcarle.konvert.converter.api.config.enforceNotNull
import io.mcarle.konvert.converter.api.config.ignoreUnmappedTargetProperties
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.exceptions.IgnoredTargetNotIgnorableException
import io.mcarle.konvert.processor.exceptions.NoMatchingTypeConverterException
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import java.util.*

class MappingCodeGenerator constructor(
    private val logger: KSPLogger
) {

    fun generateMappingCode(
        source: KSType,
        target: KSType,
        sourceProperties: List<PropertyMappingInfo>,
        constructor: KSFunctionDeclaration,
        functionParamName: String?,
        targetClassImportName: String?,
        targetProperties: List<KSPropertyDeclaration>
    ): CodeBlock {
        val className = constructor.parentDeclaration!!.simpleName.asString()
        val constructorCode = constructorCode(
            className = targetClassImportName,
            classDeclaration = constructor.parentDeclaration as? KSClassDeclaration,
            constructor = constructor,
            sourceProperties = sourceProperties
        )
        val propertyCode = propertyCode(
            className = className,
            functionParamName = functionParamName,
            sourceProperties = sourceProperties,
            targetProperties = targetProperties
        )
        return if (source.isNullable()) {
            // source can only be nullable in case of @Konverter/@Konvert which require a functionParamName
            val code = "return·${functionParamName!!}?.let·{\n⇥%L%L⇤\n}"
            if (target.isNullable()) {
                CodeBlock.of(code, constructorCode, propertyCode)
            } else {
                if (Configuration.enforceNotNull) {
                    CodeBlock.of("$code!!", constructorCode, propertyCode)
                } else {
                    throw NotNullOperatorNotEnabledException(functionParamName, source, target)
                }
            }
        } else {
            CodeBlock.of("return·%L%L", constructorCode, propertyCode)
        }
    }

    private fun constructorCode(
        className: String?,
        classDeclaration: KSClassDeclaration?,
        constructor: KSFunctionDeclaration,
        sourceProperties: List<PropertyMappingInfo>
    ): CodeBlock {
        if (className == null) {
            return if (constructor.parameters.isEmpty()) {
                CodeBlock.of("%T()", classDeclaration?.toClassName())
            } else {
                CodeBlock.of(
                    """
%T(${"⇥\n%L"}
⇤)
                    """.trimIndent(),
                    classDeclaration?.toClassName(),
                    constructorParamsCode(constructor = constructor, sourceProperties = sourceProperties)
                )
            }
        }
        return if (constructor.parameters.isEmpty()) {
            CodeBlock.of("$className()")
        } else {
            CodeBlock.of(
                """
$className(${"⇥\n%L"}
⇤)
                """.trimIndent(),
                constructorParamsCode(constructor = constructor, sourceProperties = sourceProperties)
            )
        }
    }

    private fun constructorParamsCode(
        constructor: KSFunctionDeclaration,
        sourceProperties: List<PropertyMappingInfo>
    ): CodeBlock {
        return constructor.parameters.mapNotNull { ksValueParameter ->
            val constructorHasParamNames = constructor.origin !in listOf(
                Origin.JAVA,
                Origin.JAVA_LIB
            )
            val valueParamHasDefault = ksValueParameter.hasDefault
            val valueParamIsNullable = ksValueParameter.type.resolve().isNullable()

            val propertyMappingInfo = determinePropertyMappingInfo(sourceProperties, ksValueParameter)
            val convertedValue = convertValue(
                source = propertyMappingInfo,
                targetTypeRef = ksValueParameter.type,
                valueParamHasDefault = valueParamHasDefault,
                valueParamIsNullable = valueParamIsNullable
            )

            if (convertedValue != null) {
                if (constructorHasParamNames) {
                    CodeBlock.of("${propertyMappingInfo?.targetName ?: ksValueParameter.name?.asString()}·=·%L", convertedValue)
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
            if (sourceProperty == null) {
                if (!Configuration.ignoreUnmappedTargetProperties) {
                    throw PropertyMappingNotExistingException(targetProperty, sourceProperties)
                } else {
                    logger.warn(
                        "Ignoring unmapped target property `${targetProperty.simpleName.asString()}`  due to configuration 'konvert.ignore-unmapped-target-properties=true'",
                        targetProperty
                    )
                    null
                }
            } else {
                val convertedValue = convertValue(
                    source = sourceProperty,
                    targetTypeRef = targetProperty.type,
                    valueParamIsNullable = false,
                    valueParamHasDefault = true
                )
                if (convertedValue != null) {
                    CodeBlock.of("$targetVarName.${sourceProperty.targetName}·=·$convertedValue")
                } else {
                    null
                }
            }
        }.joinToCode("\n")
    }

    private fun determinePropertyMappingInfo(
        propertyMappings: List<PropertyMappingInfo>,
        ksValueParameter: KSValueParameter
    ): PropertyMappingInfo? {
        return propertyMappings.firstOrNull {
            it.targetName == ksValueParameter.name?.asString()
        }
    }

    private fun determinePropertyMappingInfo(
        propertyMappings: List<PropertyMappingInfo>,
        ksPropertyDeclaration: KSPropertyDeclaration
    ): PropertyMappingInfo? {
        return propertyMappings.firstOrNull {
            it.targetName == ksPropertyDeclaration.simpleName.asString()
        }
    }

    private fun convertValue(
        source: PropertyMappingInfo?,
        targetTypeRef: KSTypeReference,
        valueParamHasDefault: Boolean,
        valueParamIsNullable: Boolean
    ): CodeBlock? {
        val targetType = targetTypeRef.resolve()

        return when {
            source == null -> handleNullSource(valueParamHasDefault, valueParamIsNullable, targetTypeRef)
            source.sourceData == null -> handleNullSourceData(source, valueParamHasDefault, valueParamIsNullable, targetTypeRef)
            else -> handleNonNullSourceData(source, targetType)
        }
    }

    private fun handleNullSource(
        valueParamHasDefault: Boolean,
        valueParamIsNullable: Boolean,
        targetTypeRef: KSTypeReference
    ): CodeBlock? {
        return when {
            valueParamHasDefault -> null
            valueParamIsNullable -> CodeBlock.of("null")
            else -> throw PropertyMappingNotExistingException(targetTypeRef.toString(), emptyList())
        }
    }

    private fun handleNullSourceData(
        source: PropertyMappingInfo,
        valueParamHasDefault: Boolean,
        valueParamIsNullable: Boolean,
        targetTypeRef: KSTypeReference
    ): CodeBlock? {
        return when {
            source.ignore -> handleIgnoredSource(valueParamHasDefault, valueParamIsNullable, targetTypeRef)
            source.constant != null -> CodeBlock.of(source.constant)
            source.expression != null -> {
                val expression = "let·{ ${source.expression} }"
                CodeBlock.of(
                    source.mappingParamName
                        ?.let { "$it.$expression" }
                        ?: expression
                )
            }
            else -> error("Could not convert value $source")
        }
    }

    private fun handleIgnoredSource(
        valueParamHasDefault: Boolean,
        valueParamIsNullable: Boolean,
        targetTypeRef: KSTypeReference
    ): CodeBlock? {
        return when {
            valueParamHasDefault -> null
            valueParamIsNullable -> CodeBlock.of("null")
            else -> throw IgnoredTargetNotIgnorableException(targetTypeRef.toString())
        }
    }

    private fun handleNonNullSourceData(
        source: PropertyMappingInfo,
        targetType: KSType
    ): CodeBlock {
        val sourceType = source.sourceData!!.typeRef.resolve()
        val paramName = source.mappingParamName?.let { "$it." } ?: ""

        return TypeConverterRegistry.withAdditionallyEnabledConverters(source.enableConverters + Configuration.enableConverters) {
            firstOrNull { it.matches(sourceType, targetType) }
                ?.convert(paramName + source.sourceName!!, sourceType, targetType)
                ?: throwException(paramName + source.sourceName, sourceType, source.targetName, targetType)
        }
    }

    private fun throwException(
        sourceName: String,
        sourceType: KSType,
        targetName: String,
        targetType: KSType
    ): Nothing {
        val notNullOperatorNeeded = sourceType.isNullable() && !targetType.isNullable()
        val typeConverterExisting = { TypeConverterRegistry.any { it.matches(sourceType, targetType.makeNullable()) } }

        if (notNullOperatorNeeded && !Configuration.enforceNotNull && typeConverterExisting()) {
            throw NotNullOperatorNotEnabledException(sourceName, sourceType, targetName, targetType)
        }
        throw NoMatchingTypeConverterException(sourceName, sourceType, targetName, targetType)
    }
}
