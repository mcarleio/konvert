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
import java.util.Date
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertIs

class TemporalToDateConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            InstantToDateConverter(),
            ZonedDateTimeToDateConverter(),
            OffsetDateTimeToDateConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to "java.util.Date"
        }

        private val temporalToDateConverterClasses: Set<Class<out TemporalToDateConverter>> = Reflections(TemporalToDateConverter::class.java)
            .getSubTypesOf(TemporalToDateConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(
            temporalToDateConverterClasses.newConverterInstance(simpleConverterName),
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
                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
        assertIs<Date>(targetValue)
        Assertions.assertEquals(Date.from(instant), targetValue)
    }

}

