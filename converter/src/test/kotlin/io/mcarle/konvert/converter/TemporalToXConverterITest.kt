package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertTrue

class TemporalToXConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            InstantToStringConverter(),
            ZonedDateTimeToStringConverter(),
            OffsetDateTimeToStringConverter(),
            LocalDateTimeToStringConverter(),
            LocalDateToStringConverter(),
            OffsetTimeToStringConverter(),
            LocalTimeToStringConverter(),
            InstantToLongEpochMillisConverter(),
            InstantToLongEpochSecondsConverter(),
            ZonedDateTimeToLongEpochMillisConverter(),
            ZonedDateTimeToLongEpochSecondsConverter(),
            OffsetDateTimeToLongEpochMillisConverter(),
            OffsetDateTimeToLongEpochSecondsConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to it.targetClass.qualifiedName
        }

        private val temporalToXConverterClasses: Set<Class<out TemporalToXConverter>> = Reflections(TemporalToXConverter::class.java)
            .getSubTypesOf(TemporalToXConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(
            temporalToXConverterClasses.newConverterInstance(simpleConverterName),
            sourceTypeName,
            targetTypeName
        )
    }

    override fun verifyMapper(
        converter: TypeConverter,
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
        val instant = Instant.now()
        val sourceInstance = sourceKClass.constructors.first().call(
            when {
                sourceTypeName.startsWith("java.time.Instant") -> instant
                sourceTypeName.startsWith("java.time.ZonedDateTime") -> instant.atOffset(ZoneOffset.UTC).toZonedDateTime()
                sourceTypeName.startsWith("java.time.OffsetDateTime") -> instant.atOffset(ZoneOffset.UTC)
                sourceTypeName.startsWith("java.time.LocalDateTime") -> instant.atOffset(ZoneOffset.UTC).toLocalDateTime()
                sourceTypeName.startsWith("java.time.LocalDate") -> instant.atOffset(ZoneOffset.UTC).toLocalDate()
                sourceTypeName.startsWith("java.time.OffsetTime") -> instant.atOffset(ZoneOffset.UTC).toOffsetTime()
                sourceTypeName.startsWith("java.time.LocalTime") -> instant.atOffset(ZoneOffset.UTC).toLocalTime()
                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
        when {
            targetTypeName.startsWith("kotlin.String") -> {
                targetValue as String
                assertTrue(instant.toString().contains(targetValue))
            }

            targetTypeName.startsWith("kotlin.Long") -> {
                targetValue as Long
                when (converter) {
                    is InstantToLongEpochMillisConverter,
                    is ZonedDateTimeToLongEpochMillisConverter,
                    is OffsetDateTimeToLongEpochMillisConverter -> Assertions.assertEquals(instant.toEpochMilli(), targetValue)

                    is InstantToLongEpochSecondsConverter,
                    is ZonedDateTimeToLongEpochSecondsConverter,
                    is OffsetDateTimeToLongEpochSecondsConverter -> Assertions.assertEquals(instant.toEpochMilli() / 1000, targetValue)
                }
            }
        }
    }

}

