package io.mcarle.lib.kmapper.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.api.AbstractTypeConverter
import io.mcarle.lib.kmapper.processor.api.TypeConverter
import io.mcarle.lib.kmapper.processor.api.isNullable
import kotlin.reflect.KClass

abstract class XToDateConverter(
    internal val sourceClass: KClass<*>
) : AbstractTypeConverter() {

    private val dateType: KSType by lazy {
        resolver.getClassDeclarationByName("java.util.Date")!!.asStarProjectedType()
    }

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType()
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceType == sourceNotNullable && dateType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "")

        return convertCode + appendNotNullAssertionOperatorIfNeeded(source, target)
    }

    override val enabledByDefault: Boolean = false

    abstract fun convert(fieldName: String, nc: String): String
}

@AutoService(TypeConverter::class)
class StringToDateConverter : XToDateConverter(String::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let { java.util.Date.from(java.time.Instant.parse(it)) }"
}

@AutoService(TypeConverter::class)
class LongToDateConverter : XToDateConverter(Long::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let { java.util.Date(it) }"
}