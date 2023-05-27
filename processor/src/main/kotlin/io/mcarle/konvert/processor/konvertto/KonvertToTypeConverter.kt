package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.isNullable

class KonvertToTypeConverter constructor(
    override val priority: Priority,
    internal val mapFunctionName: String,
    internal val sourceClassDeclaration: KSClassDeclaration,
    internal val targetClassDeclaration: KSClassDeclaration,
) : AbstractTypeConverter() {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override val enabledByDefault = true

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceType == sourceNotNullable && targetType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        val nc = if (source.isNullable()) "?" else ""
        return CodeBlock.of(
            "$fieldName$nc.$mapFunctionName()" + appendNotNullAssertionOperatorIfNeeded(source, target)
        )
    }

}
