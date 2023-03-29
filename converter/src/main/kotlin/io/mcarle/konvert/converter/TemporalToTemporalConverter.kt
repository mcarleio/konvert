package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
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

abstract class TemporalToTemporalConverter(
    internal val sourceClass: KClass<out Temporal>,
    internal val targetClass: KClass<out Temporal>,
) : AbstractTypeConverter() {

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType()
    }

    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asStarProjectedType()
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceType.isAssignableFrom(sourceNotNullable) && targetType == targetNotNullable
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
class OffsetDateTimeToInstantConverter : TemporalToTemporalConverter(OffsetDateTime::class, Instant::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToZonedDateTimeConverter : TemporalToTemporalConverter(OffsetDateTime::class, ZonedDateTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toZonedDateTime()"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToLocalDateTimeConverter : TemporalToTemporalConverter(OffsetDateTime::class, LocalDateTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLocalDateTime()"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToLocalDateConverter : TemporalToTemporalConverter(OffsetDateTime::class, LocalDate::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLocalDate()"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToLocalTimeConverter : TemporalToTemporalConverter(OffsetDateTime::class, LocalTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLocalTime()"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToOffsetTimeConverter : TemporalToTemporalConverter(OffsetDateTime::class, OffsetTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toOffsetTime()"
}


@AutoService(TypeConverter::class)
class ZonedDateTimeToInstantConverter : TemporalToTemporalConverter(ZonedDateTime::class, Instant::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToOffsetDateTimeConverter : TemporalToTemporalConverter(ZonedDateTime::class, OffsetDateTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toOffsetDateTime()"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToLocalDateTimeConverter : TemporalToTemporalConverter(ZonedDateTime::class, LocalDateTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLocalDateTime()"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToLocalDateConverter : TemporalToTemporalConverter(ZonedDateTime::class, LocalDate::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLocalDate()"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToLocalTimeConverter : TemporalToTemporalConverter(ZonedDateTime::class, LocalTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLocalTime()"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToOffsetTimeConverter : TemporalToTemporalConverter(ZonedDateTime::class, OffsetTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toOffsetDateTime()$nc.toOffsetTime()"
}


@AutoService(TypeConverter::class)
class LocalDateTimeToLocalDateConverter : TemporalToTemporalConverter(LocalDateTime::class, LocalDate::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLocalDate()"
}

@AutoService(TypeConverter::class)
class LocalDateTimeToLocalTimeConverter : TemporalToTemporalConverter(LocalDateTime::class, LocalTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLocalTime()"
}


@AutoService(TypeConverter::class)
class OffsetTimeToLocalTimeConverter : TemporalToTemporalConverter(OffsetTime::class, LocalTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLocalTime()"
}
