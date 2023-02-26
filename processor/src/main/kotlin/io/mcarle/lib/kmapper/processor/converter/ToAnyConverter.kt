package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.AbstractTypeConverter

class ToAnyConverter : AbstractTypeConverter() {

    private val anyType: KSType by lazy { resolver.builtIns.anyType }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { _, targetNotNullable ->
            anyType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return fieldName + appendNotNullAssertionOperatorIfNeeded(source, target)
    }

}