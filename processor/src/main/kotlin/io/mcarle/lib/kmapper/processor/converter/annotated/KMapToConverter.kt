package io.mcarle.lib.kmapper.processor.converter.annotated

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.api.annotation.KMapTo
import io.mcarle.lib.kmapper.converter.api.ConverterConfig
import io.mcarle.lib.kmapper.converter.api.Priority
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import io.mcarle.lib.kmapper.converter.api.isNullable

class KMapToConverter(
    override val annotation: KMapTo,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val mapKSClassDeclaration: KSClassDeclaration,
    val mapFunctionName: String,
) : TypeConverter, AnnotatedConverter<KMapTo> {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override val enabledByDefault: Boolean = true
    override val priority: Priority = annotation.priority

    override fun init(config: ConverterConfig) {
        // Nothing to initialize
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return sourceType in setOf(
            source,
            source.makeNotNullable()
        ) && targetType in setOf(
            target,
            target.makeNotNullable()
        )
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val nc = if (source.isNullable()) "?" else ""
        return "$fieldName$nc.$mapFunctionName()"
    }

}
