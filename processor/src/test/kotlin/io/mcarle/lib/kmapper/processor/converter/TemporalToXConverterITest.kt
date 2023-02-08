package io.mcarle.lib.kmapper.processor.converter

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
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
            InstantToLongConverter(),
            ZonedDateTimeToLongConverter(),
            OffsetDateTimeToLongConverter(),
            InstantToDateConverter(),
            ZonedDateTimeToDateConverter(),
            OffsetDateTimeToDateConverter(),
        ).toConverterTestArguments {
            it.sourceClass.qualifiedName to it.targetClass.qualifiedName
        }
    }

    @ParameterizedTest()
    @MethodSource("converterList")
    override fun converterTest(converter: TypeConverter, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(converter, sourceTypeName, targetTypeName)
    }

    override fun verifyMapper(
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
//                    Assertions.assertEquals(instant.toString(), targetValue)
            }
            targetTypeName.startsWith("kotlin.Long") -> {
                targetValue as Long
                Assertions.assertEquals(instant.toEpochMilli(), targetValue)
            }
            targetTypeName.startsWith("java.util.Date") -> {
                targetValue as Date
                Assertions.assertEquals(Date.from(instant), targetValue)
            }
            else -> null
        }
    }

}

