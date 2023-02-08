package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.isNullable

class SameTypeConverter : AbstractTypeConverter() {

    override fun matches(source: KSType, target: KSType): Boolean {
        if (source == target || target.isAssignableFrom(source)) {
            return true
        }
        val nonNullableSource = source.makeNotNullable()
        val nonNullableTarget = target.makeNotNullable()

        return nonNullableSource == nonNullableTarget || nonNullableTarget.isAssignableFrom(nonNullableSource)
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return fieldName + if (source.isNullable() && !target.isNullable()) "!!" else ""
    }
}