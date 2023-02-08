package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.isNullable

class ToAnyConverter : AbstractTypeConverter() {

    private val targetType: KSType by lazy {
        resolver.builtIns.anyType
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return target == targetType || target.makeNotNullable() == targetType
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return fieldName + if (source.isNullable() && !target.isNullable()) {
            "!!"
        } else {
            ""
        }
    }

}