package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.api.DEFAULT_PRIORITY
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.AbstractTypeConverter
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

abstract class TemporalToXConverter(
    val sourceClass: KClass<out Temporal>,
    val targetClass: KClass<*>,
) : AbstractTypeConverter() {

    private val temporalType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType()
    }

    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asStarProjectedType()
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            temporalType.isAssignableFrom(sourceNotNullable) && targetType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "")

        return convertCode + appendNotNullAssertionOperatorIfNeeded(source, target)
    }

    override val enabledByDefault: Boolean = true

    abstract fun convert(fieldName: String, nc: String): String
}

@AutoService(TypeConverter::class)
class InstantToStringConverter : TemporalToXConverter(Instant::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToStringConverter : TemporalToXConverter(ZonedDateTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToStringConverter : TemporalToXConverter(OffsetDateTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class LocalDateTimeToStringConverter : TemporalToXConverter(LocalDateTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class LocalDateToStringConverter : TemporalToXConverter(LocalDate::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class OffsetTimeToStringConverter : TemporalToXConverter(OffsetTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class LocalTimeToStringConverter : TemporalToXConverter(LocalTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}


@AutoService(TypeConverter::class)
class InstantToLongEpochMillisConverter : TemporalToXConverter(Instant::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toEpochMilli()"
}

@AutoService(TypeConverter::class)
class InstantToLongEpochSecondsConverter : TemporalToXConverter(Instant::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ it.toEpochMilli()·/·1000 }"
    override val enabledByDefault: Boolean = false
    override val priority: Priority = DEFAULT_PRIORITY - 1 // if enabled, it should have priority over InstantToLongEpochMillisConverter
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToLongEpochMillisConverter : TemporalToXConverter(ZonedDateTime::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()$nc.toEpochMilli()"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToLongEpochSecondsConverter : TemporalToXConverter(ZonedDateTime::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toEpochSecond()"
    override val enabledByDefault: Boolean = false
    override val priority: Priority =
        DEFAULT_PRIORITY - 1 // if enabled, it should have priority over ZonedDateTimeToLongEpochMillisConverter
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToLongEpochMillisConverter : TemporalToXConverter(OffsetDateTime::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()$nc.toEpochMilli()"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToLongEpochSecondsConverter : TemporalToXConverter(OffsetDateTime::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toEpochSecond()"
    override val enabledByDefault: Boolean = false
    override val priority: Priority =
        DEFAULT_PRIORITY - 1 // if enabled, it should have priority over OffsetDateTimeToLongEpochMillisConverter
}
