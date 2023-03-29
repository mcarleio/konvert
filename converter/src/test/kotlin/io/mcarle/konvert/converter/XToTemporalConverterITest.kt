package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

class XToTemporalConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            StringToInstantConverter(),
            StringToZonedDateTimeConverter(),
            StringToOffsetDateTimeConverter(),
            StringToLocalDateTimeConverter(),
            StringToLocalDateConverter(),
            StringToOffsetTimeConverter(),
            StringToLocalTimeConverter(),
            LongEpochMillisToInstantConverter(),
            LongEpochSecondsToInstantConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to it.targetClass.qualifiedName
        }

        private val xToTemporalConverterClasses: Set<Class<out XToTemporalConverter>> = Reflections(XToTemporalConverter::class.java)
            .getSubTypesOf(XToTemporalConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(xToTemporalConverterClasses.newConverterInstance(simpleConverterName), sourceTypeName, targetTypeName)
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
        val temporal: Temporal = when {
            targetTypeName.startsWith("java.time.Instant") -> Instant.now()
            targetTypeName.startsWith("java.time.ZonedDateTime") -> ZonedDateTime.now()
            targetTypeName.startsWith("java.time.OffsetDateTime") -> OffsetDateTime.now()
            targetTypeName.startsWith("java.time.LocalDateTime") -> LocalDateTime.now()
            targetTypeName.startsWith("java.time.LocalDate") -> LocalDate.now()
            targetTypeName.startsWith("java.time.OffsetTime") -> OffsetTime.now()
            targetTypeName.startsWith("java.time.LocalTime") -> LocalTime.now()
            else -> LocalDate.now()
        }

        val sourceInstance = sourceKClass.constructors.first().call(
            when {
                sourceTypeName.startsWith("kotlin.String") -> temporal.toString()
                sourceTypeName.startsWith("kotlin.Long") -> when (converter) {
                    is LongEpochMillisToInstantConverter -> temporal.getLong(ChronoField.INSTANT_SECONDS) * 1000 + temporal.getLong(
                        ChronoField.MILLI_OF_SECOND
                    )

                    is LongEpochSecondsToInstantConverter -> temporal.getLong(ChronoField.INSTANT_SECONDS)
                    else -> null
                }

                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
        when {
            targetTypeName.startsWith("java.time.Instant") -> when {
                sourceTypeName.startsWith("kotlin.String") -> assertEquals(temporal, targetValue)
                sourceTypeName.startsWith("kotlin.Long") -> when (converter) {
                    is LongEpochMillisToInstantConverter -> assertEquals(
                        (temporal as Instant).truncatedTo(ChronoUnit.MILLIS),
                        targetValue
                    )

                    is LongEpochSecondsToInstantConverter -> assertEquals(
                        (temporal as Instant).truncatedTo(ChronoUnit.SECONDS),
                        targetValue
                    )

                    else -> null
                }
            }

            targetTypeName.startsWith("java.time.ZonedDateTime") -> assertEquals(temporal, targetValue)
            targetTypeName.startsWith("java.time.OffsetDateTime") -> assertEquals(temporal, targetValue)
            targetTypeName.startsWith("java.time.LocalDateTime") -> assertEquals(temporal, targetValue)
            targetTypeName.startsWith("java.time.LocalDate") -> assertEquals(temporal, targetValue)
        }
    }

}

