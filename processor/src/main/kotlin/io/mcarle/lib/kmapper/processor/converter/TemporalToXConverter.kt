package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.isNullable
import java.time.*
import java.time.temporal.Temporal
import java.util.*
import kotlin.reflect.KClass

abstract class TemporalToXConverter<T : Temporal>(
    internal val sourceClass: KClass<T>,
    internal val targetClass: KClass<*>,
) : AbstractTypeConverter() {

    private val temporalType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType().makeNullable()
    }

    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asType(emptyList())
    }

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asType(emptyList())
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return temporalType.isAssignableFrom(source) && (targetType == target || targetType == target.makeNotNullable())
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "")

        return if (sourceNullable && !target.isNullable()) {
            "$convertCode!!"
        } else {
            convertCode
        }
    }

    abstract fun convert(fieldName: String, nc: String): String
}

class InstantToStringConverter : TemporalToXConverter<Instant>(Instant::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class ZonedDateTimeToStringConverter : TemporalToXConverter<ZonedDateTime>(ZonedDateTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class OffsetDateTimeToStringConverter : TemporalToXConverter<OffsetDateTime>(OffsetDateTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class LocalDateTimeToStringConverter : TemporalToXConverter<LocalDateTime>(LocalDateTime::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class LocalDateToStringConverter : TemporalToXConverter<LocalDate>(LocalDate::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}


class InstantToLongConverter : TemporalToXConverter<Instant>(Instant::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toEpochMilli()"
}

class ZonedDateTimeToLongConverter : TemporalToXConverter<ZonedDateTime>(ZonedDateTime::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()$nc.toEpochMilli()"
}

class OffsetDateTimeToLongConverter : TemporalToXConverter<OffsetDateTime>(OffsetDateTime::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()$nc.toEpochMilli()"
}

class InstantToDateConverter : TemporalToXConverter<Instant>(Instant::class, Date::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let { java.util.Date.from(it) }"
}

class ZonedDateTimeToDateConverter : TemporalToXConverter<ZonedDateTime>(ZonedDateTime::class, Date::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let { java.util.Date.from(it.toInstant()) }"
}

class OffsetDateTimeToDateConverter : TemporalToXConverter<OffsetDateTime>(OffsetDateTime::class, Date::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let { java.util.Date.from(it.toInstant()) }"
}