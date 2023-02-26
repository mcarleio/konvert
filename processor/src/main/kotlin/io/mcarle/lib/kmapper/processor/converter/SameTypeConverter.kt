package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.AbstractTypeConverter

class SameTypeConverter : AbstractTypeConverter() {

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceNotNullable == targetNotNullable || targetNotNullable.isAssignableFrom(sourceNotNullable)
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return fieldName + appendNotNullAssertionOperatorIfNeeded(source, target)
    }
}