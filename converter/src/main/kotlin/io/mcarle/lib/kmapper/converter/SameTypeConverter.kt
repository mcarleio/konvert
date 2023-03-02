package io.mcarle.lib.kmapper.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.api.AbstractTypeConverter
import io.mcarle.lib.kmapper.processor.api.TypeConverter

@AutoService(TypeConverter::class)
class SameTypeConverter : AbstractTypeConverter() {
    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceNotNullable == targetNotNullable || targetNotNullable.isAssignableFrom(sourceNotNullable)
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return fieldName + appendNotNullAssertionOperatorIfNeeded(source, target)
    }
}