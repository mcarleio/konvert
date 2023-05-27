package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.TypeConverter

@AutoService(TypeConverter::class)
class ToAnyConverter : AbstractTypeConverter() {

    private val anyType: KSType by lazy { resolver.builtIns.anyType }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { _, targetNotNullable ->
            anyType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        return CodeBlock.of(
            fieldName + appendNotNullAssertionOperatorIfNeeded(source, target)
        )
    }

    override val enabledByDefault: Boolean = true

}
