package io.mcarle.lib.kmapper.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import io.mcarle.lib.kmapper.converter.api.isNullable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import kotlin.reflect.KClass

abstract class XToTemporalConverter<T : Temporal>(
    internal val sourceClass: KClass<*>,
    internal val targetClass: KClass<T>,
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
class StringToInstantConverter : XToTemporalConverter<Instant>(String::class, Instant::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.time.Instant.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToZonedDateTimeConverter : XToTemporalConverter<ZonedDateTime>(String::class, ZonedDateTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.ZonedDateTime.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToOffsetDateTimeConverter : XToTemporalConverter<OffsetDateTime>(String::class, OffsetDateTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.OffsetDateTime.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToLocalDateTimeConverter : XToTemporalConverter<LocalDateTime>(String::class, LocalDateTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.LocalDateTime.parse(it) }"
}

@AutoService(TypeConverter::class)
class StringToLocalDateConverter : XToTemporalConverter<LocalDate>(String::class, LocalDate::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.LocalDate.parse(it) }"
}

@AutoService(TypeConverter::class)
class LongToInstantConverter : XToTemporalConverter<Instant>(Long::class, Instant::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let·{ java.time.Instant.ofEpochMilli(it) }"
}