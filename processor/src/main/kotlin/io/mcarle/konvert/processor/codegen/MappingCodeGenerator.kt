package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.enableConverters
import io.mcarle.konvert.converter.api.config.enforceNotNull
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.exceptions.IgnoredTargetNotIgnorableException
import io.mcarle.konvert.processor.exceptions.NoMatchingTypeConverterException
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy.TargetSetter
import java.util.Locale

class MappingCodeGenerator {

    fun generateMappingCode(
        context: MappingContext,
        sourceProperties: List<PropertyMappingInfo>,
        constructor: KSFunctionDeclaration,
        targetProperties: List<KSPropertyDeclaration>,
        targetSetters: List<TargetSetter>
    ): CodeBlock {
        val className = constructor.parentDeclaration!!.simpleName.asString()
        val constructorCode = constructorCode(
            className = context.targetClassImportName,
            classDeclaration = context.targetClassDeclaration,
            constructor = constructor,
            sourceProperties = sourceProperties
        )
        val propertyCode = propertyCode(
            className = className,
            functionParamName = context.paramName,
            sourceProperties = sourceProperties,
            targetProperties = targetProperties,
            targetSetters = targetSetters
        )
        return if (context.source.isNullable()) {
            // source can only be nullable in case of @Konverter/@Konvert which require a functionParamName
            val code = "return·${context.paramName!!}?.let·{\n⇥%L%L⇤\n}"
            if (context.target.isNullable()) {
                CodeBlock.of(code, constructorCode, propertyCode)
            } else {
                if (Configuration.enforceNotNull) {
                    CodeBlock.of("$code!!", constructorCode, propertyCode)
                } else {
                    throw NotNullOperatorNotEnabledException(context.paramName, context.source, context.target)
                }
            }
        } else {
            CodeBlock.of("return·%L%L", constructorCode, propertyCode)
        }
    }

    private fun constructorCode(
        className: String?,
        classDeclaration: KSClassDeclaration,
        constructor: KSFunctionDeclaration,
        sourceProperties: List<PropertyMappingInfo>
    ): CodeBlock {
        if (className == null) {
            return if (constructor.parameters.isEmpty()) {
                CodeBlock.of("%T()", classDeclaration.toClassName())
            } else {
                CodeBlock.of(
                    """
%T(${"⇥\n%L"}
⇤)
                    """.trimIndent(),
                    classDeclaration.toClassName(),
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
        targetProperties: List<KSPropertyDeclaration>,
        targetSetters: List<TargetSetter>
    ): CodeBlock {
        if (noTargetOrAllIgnored(sourceProperties, targetProperties, targetSetters)) return CodeBlock.of("")

        var varName = className.replaceFirstChar { it.lowercase(Locale.getDefault()) }
        if (varName == functionParamName) {
            varName += "0"
        }

        return CodeBlock.of(
            """
.also·{·$varName·->${"⇥\n%L"}
⇤}
        """.trimIndent(), propertySettingCode(targetProperties, targetSetters, sourceProperties, varName)
        )
    }

    private fun noTargetOrAllIgnored(
        sourceProperties: List<PropertyMappingInfo>,
        targetProperties: List<KSPropertyDeclaration>,
        targetSetters: List<TargetSetter>
    ): Boolean {
        return targetProperties.all { targetProperty ->
            sourceProperties.any { sourceProperty ->
                sourceProperty.ignore
                    && sourceProperty.targetName == targetProperty.simpleName.asString()
            }
        } && targetSetters.all { targetSetter ->
            sourceProperties.any { sourceProperty ->
                sourceProperty.ignore
                    && sourceProperty.targetName == targetSetter.name
            }
        }
    }

    private fun propertySettingCode(
        targetProperties: List<KSPropertyDeclaration>,
        targetSetters: List<TargetSetter>,
        sourceProperties: List<PropertyMappingInfo>,
        targetVarName: String
    ): CodeBlock {
        val propertyCodeBlocks = targetProperties.mapNotNull { targetProperty ->
            val sourceProperty = determinePropertyMappingInfo(sourceProperties, targetProperty)
            val convertedValue = convertValue(
                source = sourceProperty,
                targetTypeRef = targetProperty.type,
                valueParamIsNullable = false,
                valueParamHasDefault = true
            )
            if (convertedValue != null) {
                CodeBlock.of("$targetVarName.${sourceProperty.targetName}·=·%L", convertedValue)
            } else {
                null
            }
        }
        val setterCodeBlocks = targetSetters.mapNotNull { targetSetter ->
            val sourceProperty = determinePropertyMappingInfo(sourceProperties, targetSetter)
            val convertedValue = convertValue(
                source = sourceProperty,
                targetTypeRef = targetSetter.typeRef,
                valueParamIsNullable = false,
                valueParamHasDefault = true
            )
            if (convertedValue != null) {
                CodeBlock.of("$targetVarName.%L", targetSetter.generateAssignmentCode(convertedValue))
            } else {
                null
            }
        }


        return (propertyCodeBlocks + setterCodeBlocks).joinToCode("\n")
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
    ): PropertyMappingInfo {
        return propertyMappings.firstOrNull {
            it.targetName == ksPropertyDeclaration.simpleName.asString()
        } ?: throw PropertyMappingNotExistingException(ksPropertyDeclaration, propertyMappings)
    }

    private fun determinePropertyMappingInfo(
        propertyMappings: List<PropertyMappingInfo>,
        setter: TargetSetter
    ): PropertyMappingInfo {
        return propertyMappings.firstOrNull {
            it.targetName == setter.name
        } ?: throw PropertyMappingNotExistingException(setter.name, propertyMappings)
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
