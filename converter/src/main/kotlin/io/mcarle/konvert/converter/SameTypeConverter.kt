package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.api.SAME_TYPE_PRIORITY
import io.mcarle.konvert.converter.api.AbstractTypeConverter
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

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        return applyNotNullEnforcementIfNeeded(fieldName, source, target)
    }
}
