package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.api.DEFAULT_PRIORITY
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.isNullable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import kotlin.reflect.KClass

abstract class XToTemporalConverter(
    val sourceClass: KClass<*>,
    val targetClass: KClass<out Temporal>,
) : AbstractTypeConverter() {

    private val temporalType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asStarProjectedType()
    }

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType()
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceType == sourceNotNullable && temporalType == targetNotNullable
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
class StringToInstantConverter : XToTemporalConverter(String::class, Instant::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.time.Instant.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToZonedDateTimeConverter : XToTemporalConverter(String::class, ZonedDateTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.ZonedDateTime.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToOffsetDateTimeConverter : XToTemporalConverter(String::class, OffsetDateTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.OffsetDateTime.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToLocalDateTimeConverter : XToTemporalConverter(String::class, LocalDateTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.LocalDateTime.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToLocalDateConverter : XToTemporalConverter(String::class, LocalDate::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.LocalDate.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToOffsetTimeConverter : XToTemporalConverter(String::class, OffsetTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.OffsetTime.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToLocalTimeConverter : XToTemporalConverter(String::class, LocalTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.LocalTime.parse(it) }"
}

@AutoService(TypeConverter::class)
class LongEpochMillisToInstantConverter : XToTemporalConverter(Long::class, Instant::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.Instant.ofEpochMilli(it) }"
}

@AutoService(TypeConverter::class)
class LongEpochSecondsToInstantConverter : XToTemporalConverter(Long::class, Instant::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.Instant.ofEpochSecond(it) }"

    override val priority: Priority = DEFAULT_PRIORITY + 1
}
