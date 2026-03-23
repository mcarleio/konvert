package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.isNullable

@AutoService(TypeConverter::class)
class ArrayToArrayConverter : BaseArrayConverter() {

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            val sourceArrayElementType = sourceArrayElementType(sourceNotNullable)?.first
            val targetArrayElementType = targetArrayElementType(targetNotNullable)?.first

            sourceArrayElementType != null && targetArrayElementType != null && TypeConverterRegistry.any {
                it.matches(
                    source = sourceArrayElementType,
                    target = targetArrayElementType,
                )
            }
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {

        val sourceNotNullable = source.makeNotNullable()
        val (genericSourceType, sourceVariance, sourceIsPrimitiveArray) = sourceArrayElementType(sourceNotNullable)!!

        val targetNotNullable = target.makeNotNullable()
        val (genericTargetType, targetVariance, targetIsPrimitiveArray) = targetArrayElementType(targetNotNullable)!!

        val nc = if (source.isNullable()) "?" else ""

        if (targetIsPrimitiveArray == sourceIsPrimitiveArray) {
            if (sourceVariance == targetVariance || targetVariance == Variance.STAR || sourceVariance == Variance.INVARIANT) {
                if (genericSourceType == genericTargetType) {
                    return applyNotNullEnforcementIfNeeded(
                        expression = CodeBlock.of(fieldName),
                        fieldName = fieldName,
                        source = source,
                        target = target
                    )
                } else if (genericSourceType.makeNullable() == genericTargetType) {
                    return applyNotNullEnforcementIfNeeded(
                        expression = CodeBlock.of("$fieldName$nc.copyOf($fieldName.size)"),
                        fieldName = fieldName,
                        source = source,
                        target = target
                    )
                }
            }
        } else if (targetIsPrimitiveArray && sourceVariance == targetVariance && genericSourceType == genericTargetType) {
            return applyNotNullEnforcementIfNeeded(
                expression = CodeBlock.of("$fieldName$nc.to${genericTargetType.toClassName().simpleName}Array()"),
                fieldName = fieldName,
                source = source,
                target = target
            )
        }

        val typeConverter = TypeConverterRegistry.first {
            it.matches(
                source = genericSourceType,
                target = genericTargetType,
            )
        }


        val mappingCodeBlock = CodeBlock.of(
            "$fieldName$nc.map·{ %L }",
            typeConverter.convert("it", genericSourceType, genericTargetType)
        )
        val codeBlock = if (targetIsPrimitiveArray) {
            CodeBlock.of("%L$nc.to${genericTargetType.toClassName().simpleName}Array()", mappingCodeBlock)
        } else {
            CodeBlock.of("%L$nc.toTypedArray()", mappingCodeBlock)
        }
        return applyNotNullEnforcementIfNeeded(
            codeBlock,
            fieldName,
            source,
            target
        )
    }

}
