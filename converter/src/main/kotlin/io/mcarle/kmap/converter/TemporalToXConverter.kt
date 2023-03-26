package io.mcarle.kmap.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.kmap.converter.api.TypeConverter
import io.mcarle.kmap.converter.api.isNullable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import java.util.Date
import kotlin.reflect.KClass

abstract class TemporalToXConverter<T : Temporal>(
    internal val sourceClass: KClass<T>,
    internal val targetClass: KClass<*>,
) : AbstractTypeConverter() {

    private val temporalType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType()
    }

    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asStarProjectedType()
    }

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType()
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
class InstantToStringConverter : TemporalToXConverter<Instant>(Instant::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToStringConverter : TemporalToXConverter<ZonedDateTime>(ZonedDateTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToStringConverter : TemporalToXConverter<OffsetDateTime>(OffsetDateTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class LocalDateTimeToStringConverter : TemporalToXConverter<LocalDateTime>(LocalDateTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class LocalDateToStringConverter : TemporalToXConverter<LocalDate>(LocalDate::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}


@AutoService(TypeConverter::class)
class InstantToLongConverter : TemporalToXConverter<Instant>(Instant::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toEpochMilli()"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToLongConverter : TemporalToXConverter<ZonedDateTime>(ZonedDateTime::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()$nc.toEpochMilli()"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToLongConverter : TemporalToXConverter<OffsetDateTime>(OffsetDateTime::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()$nc.toEpochMilli()"
}

@AutoService(TypeConverter::class)
class InstantToDateConverter : TemporalToXConverter<Instant>(Instant::class, Date::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.util.Date.from(it) }"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToDateConverter : TemporalToXConverter<ZonedDateTime>(ZonedDateTime::class, Date::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.util.Date.from(it.toInstant()) }"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToDateConverter : TemporalToXConverter<OffsetDateTime>(OffsetDateTime::class, Date::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.util.Date.from(it.toInstant()) }"
}