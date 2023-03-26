package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.converter.api.Priority
import io.mcarle.konvert.converter.api.SAME_TYPE_PRIORITY
import io.mcarle.konvert.converter.api.TypeConverter

@AutoService(TypeConverter::class)
class SameTypeConverter : AbstractTypeConverter() {
    override val enabledByDefault: Boolean = true
    override val priority: Priority = SAME_TYPE_PRIORITY

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceNotNullable == targetNotNullable || targetNotNullable.isAssignableFrom(sourceNotNullable)
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return fieldName + appendNotNullAssertionOperatorIfNeeded(source, target)
    }
}
