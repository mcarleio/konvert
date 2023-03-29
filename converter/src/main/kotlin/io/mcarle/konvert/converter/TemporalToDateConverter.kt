package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.isNullable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import kotlin.reflect.KClass

abstract class TemporalToDateConverter(
    internal val sourceClass: KClass<out Temporal>,
) : AbstractTypeConverter() {

    private val temporalType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType()
    }

    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName("java.util.Date")!!.asStarProjectedType()
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
class InstantToDateConverter : TemporalToDateConverter(Instant::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.util.Date.from(it) }"
}

@AutoService(TypeConverter::class)
class ZonedDateTimeToDateConverter : TemporalToDateConverter(ZonedDateTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.util.Date.from(it.toInstant()) }"
}

@AutoService(TypeConverter::class)
class OffsetDateTimeToDateConverter : TemporalToDateConverter(OffsetDateTime::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.util.Date.from(it.toInstant()) }"
}
