package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.isNullable

@AutoService(TypeConverter::class)
class IterableToArrayConverter : BaseArrayConverter() {

    private val iterableType: KSType by lazy { resolver.builtIns.iterableType }

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            val targetArrayElementType = targetArrayElementType(targetNotNullable)?.first

            targetArrayElementType != null && iterableType.isAssignableFrom(sourceNotNullable) && TypeConverterRegistry.any {
                it.matches(
                    source = source.arguments[0].type?.resolve() ?: resolver.builtIns.anyType,
                    target = targetArrayElementType,
                )
            }
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        val genericSourceType = source.arguments[0].type?.resolve() ?: resolver.builtIns.anyType

        val targetNotNullable = target.makeNotNullable()
        val (genericTargetType, _, targetIsPrimitiveArray) = targetArrayElementType(targetNotNullable)!!

        val nc = if (source.isNullable()) "?" else ""
        if (genericTargetType.isAssignableFrom(genericSourceType)) {
            return if (targetIsPrimitiveArray) {
                applyNotNullEnforcementIfNeeded(
                    expression = CodeBlock.of("$fieldName$nc.to${genericTargetType.toClassName().simpleName}Array()"),
                    fieldName = fieldName,
                    source = source,
                    target = target
                )
            } else {
                applyNotNullEnforcementIfNeeded(
                    expression = CodeBlock.of("$fieldName$nc.toTypedArray()"),
                    fieldName = fieldName,
                    source = source,
                    target = target
                )
            }
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
