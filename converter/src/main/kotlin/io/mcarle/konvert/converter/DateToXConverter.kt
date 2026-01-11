package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.api.DEFAULT_PRIORITY
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.isNullable
import kotlin.reflect.KClass

abstract class DateToXConverter(
    val targetClass: KClass<*>
) : AbstractTypeConverter() {

    private val dateType: KSType by lazy {
        resolver.getClassDeclarationByName("java.util.Date")!!.asStarProjectedType()
    }

    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asStarProjectedType()
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            dateType.isAssignableFrom(sourceNotNullable) && targetType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        val nc = if (source.isNullable()) "?" else ""
        val expression = CodeBlock.of("%L", convert(fieldName, nc))

        return applyNotNullEnforcementIfNeeded(
            expression = expression,
            fieldName = fieldName,
            source = source,
            target = target
        )
    }

    abstract fun convert(fieldName: String, nc: String): String
}

@AutoService(TypeConverter::class)
class DateToStringConverter : DateToXConverter(String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()$nc.toString()"
    override val enabledByDefault = true
}

@AutoService(TypeConverter::class)
class DateToLongEpochMillisConverter : DateToXConverter(Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.time"
    override val enabledByDefault = true
}

@AutoService(TypeConverter::class)
class DateToLongEpochSecondsConverter : DateToXConverter(Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ it.time·/·1000 }"
    override val enabledByDefault = false
    override val priority: Priority = DEFAULT_PRIORITY - 1 // if enabled, it should have priority over DateToLongEpochMillisConverter
}
