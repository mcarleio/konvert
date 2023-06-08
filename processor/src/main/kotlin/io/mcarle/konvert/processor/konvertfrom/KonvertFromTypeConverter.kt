package io.mcarle.konvert.processor.konvertfrom

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter

class KonvertFromTypeConverter constructor(
    override val priority: Priority,
    override val alreadyGenerated: Boolean,
    internal val mapFunctionName: String,
    internal val paramName: String,
    internal val sourceClassDeclaration: KSClassDeclaration,
    internal val targetClassDeclaration: KSClassDeclaration,
) : AbstractTypeConverter(), AnnotatedConverter {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceType == sourceNotNullable && targetType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        return CodeBlock.of(
            if (source.isNullable()) {
                "$fieldName?.let·{ %T.%M($paramName·=·it) }"
            } else {
                "%T.%M($paramName·=·$fieldName)"
            } + appendNotNullAssertionOperatorIfNeeded(source, target),
            targetClassDeclaration.toClassName(),
            MemberName(targetClassDeclaration.packageName.asString(), mapFunctionName)
        )
    }
}
