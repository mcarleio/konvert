package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Origin
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.EnforceNotNullStrategy
import io.mcarle.konvert.converter.api.config.enableConverters
import io.mcarle.konvert.converter.api.config.enforceNotNull
import io.mcarle.konvert.converter.api.config.enforceNotNullStrategy
import io.mcarle.konvert.processor.ResolvedType
import io.mcarle.konvert.processor.exceptions.IgnoredTargetNotIgnorableException
import io.mcarle.konvert.processor.exceptions.NoMatchingTypeConverterException
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.exceptions.PropertyMappingNotExistingException
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy.TargetConstructor
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy.TargetSetter
import io.mcarle.konvert.processor.targetdata.TargetDataExtractionStrategy.TargetVarProperty
import java.util.Locale

class MappingCodeGenerator constructor(
    private val logger: KSPLogger
) {

    fun generateMappingCode(
        context: MappingContext,
        sourceProperties: List<PropertyMappingInfo>,
        constructor: TargetConstructor,
        targetProperties: List<TargetVarProperty>,
        targetSetters: List<TargetSetter>
    ): CodeBlock {
        val className = constructor.className
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
                    when (Configuration.enforceNotNullStrategy) {
                        EnforceNotNullStrategy.ASSERTION_OPERATOR ->
                            CodeBlock.of("$code!!", constructorCode, propertyCode)

                        EnforceNotNullStrategy.REQUIRE_NOT_NULL -> {
                            val requireCode =
                                "return·requireNotNull(${context.paramName})·{·\"${context.paramName}·must·not·be·null\"·}·.let·{\n⇥%L%L⇤\n}"
                            CodeBlock.of(requireCode, constructorCode, propertyCode)
                        }
                    }
                } else {
                    throw NotNullOperatorNotEnabledException(context.paramName, context.source.ksType, context.target.ksType)
                }
            }
        } else {
            CodeBlock.of("return·%L%L", constructorCode, propertyCode)
        }
    }

    private fun constructorCode(
        className: String?,
        classDeclaration: KSClassDeclaration,
        constructor: TargetConstructor,
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
        constructor: TargetConstructor,
        sourceProperties: List<PropertyMappingInfo>
    ): CodeBlock {
        return constructor.parameters.mapNotNull { constructorParameter ->
            val constructorHasParamNames = constructor.origin !in listOf(
                Origin.JAVA,
                Origin.JAVA_LIB
            )

            val valueParamHasDefault = constructorParameter.hasDefault
            val valueParamIsNullable = constructorParameter.type.isNullable()

            val propertyMappingInfo = determinePropertyMappingInfo(sourceProperties, constructorParameter)
            val convertedValue = convertValue(
                source = propertyMappingInfo,
                targetType = constructorParameter.type,
                valueParamHasDefault = valueParamHasDefault,
                valueParamIsNullable = valueParamIsNullable
            )

            if (convertedValue != null) {
                if (constructorHasParamNames) {
                    CodeBlock.of("${propertyMappingInfo?.targetName ?: constructorParameter.name}·=·%L", convertedValue)
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
        targetProperties: List<TargetVarProperty>,
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
        targetProperties: List<TargetVarProperty>,
        targetSetters: List<TargetSetter>
    ): Boolean {
        return targetProperties.all { targetProperty ->
            sourceProperties.any { sourceProperty ->
                sourceProperty.ignore
                    && sourceProperty.targetName == targetProperty.name
            }
        } && targetSetters.all { targetSetter ->
            sourceProperties.any { sourceProperty ->
                sourceProperty.ignore
                    && sourceProperty.targetName == targetSetter.name
            }
        }
    }

    private fun propertySettingCode(
        targetProperties: List<TargetVarProperty>,
        targetSetters: List<TargetSetter>,
        sourceProperties: List<PropertyMappingInfo>,
        targetVarName: String
    ): CodeBlock {
        val propertyCodeBlocks = targetProperties.mapNotNull { targetProperty ->
            val sourceProperty = determinePropertyMappingInfo(sourceProperties, targetProperty)
            val convertedValue = convertValue(
                source = sourceProperty,
                targetType = targetProperty.type,
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
                targetType = targetSetter.type,
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
        targetConstructorParameter: TargetDataExtractionStrategy.TargetConstructorParameter
    ): PropertyMappingInfo? {
        return propertyMappings.firstOrNull {
            it.targetName == targetConstructorParameter.name
        }
    }

    private fun determinePropertyMappingInfo(
        propertyMappings: List<PropertyMappingInfo>,
        targetProperty: TargetVarProperty
    ): PropertyMappingInfo {
        return propertyMappings.firstOrNull {
            it.targetName == targetProperty.name
        } ?: throw PropertyMappingNotExistingException(targetProperty.name, propertyMappings)
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
        targetType: ResolvedType,
        valueParamHasDefault: Boolean,
        valueParamIsNullable: Boolean
    ): CodeBlock? {
        return when {
            source == null -> handleNullSource(valueParamHasDefault, valueParamIsNullable, targetType)
            source.sourceData == null -> handleNullSourceData(source, valueParamHasDefault, valueParamIsNullable, targetType)
            else -> handleNonNullSourceData(source, targetType)
        }
    }

    private fun handleNullSource(
        valueParamHasDefault: Boolean,
        valueParamIsNullable: Boolean,
        targetType: ResolvedType
    ): CodeBlock? {
        return when {
            valueParamHasDefault -> null
            valueParamIsNullable -> CodeBlock.of("null")
            else -> throw PropertyMappingNotExistingException(targetType.toString(), emptyList())
        }
    }

    private fun handleNullSourceData(
        source: PropertyMappingInfo,
        valueParamHasDefault: Boolean,
        valueParamIsNullable: Boolean,
        targetType: ResolvedType
    ): CodeBlock? {
        return when {
            source.ignore -> handleIgnoredSource(valueParamHasDefault, valueParamIsNullable, targetType)
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
        targetType: ResolvedType
    ): CodeBlock? {
        return when {
            valueParamHasDefault -> null
            valueParamIsNullable -> CodeBlock.of("null")
            else -> throw IgnoredTargetNotIgnorableException(targetType.toString())
        }
    }

    private fun handleNonNullSourceData(
        source: PropertyMappingInfo,
        targetType: ResolvedType
    ): CodeBlock {
        val sourceData = source.sourceData!!
        val sourceType = sourceData.type
        val paramName = source.mappingParamName?.let { "$it." } ?: ""
        val sourceAccessCode = sourceData.accessCode

        return TypeConverterRegistry.withAdditionallyEnabledConverters(source.enableConverters + Configuration.enableConverters) {
            firstOrNull { it.matches(sourceType.ksType, targetType.ksType) }
                ?.convert(paramName + sourceAccessCode, sourceType.ksType, targetType.ksType)
                ?: throwException(paramName + source.sourceName, sourceType, source.targetName, targetType)
        }
    }

    private fun throwException(
        sourceName: String,
        sourceType: ResolvedType,
        targetName: String,
        targetType: ResolvedType
    ): Nothing {
        val notNullOperatorNeeded = sourceType.isNullable() && !targetType.isNullable()
        val typeConverterExisting = { TypeConverterRegistry.any { it.matches(sourceType.ksType, targetType.ksType.makeNullable()) } }

        if (notNullOperatorNeeded && !Configuration.enforceNotNull && typeConverterExisting()) {
            throw NotNullOperatorNotEnabledException(sourceName, sourceType.ksType, targetName, targetType.ksType)
        }
        throw NoMatchingTypeConverterException(sourceName, sourceType.ksType, targetName, targetType.ksType)
    }
}
