package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import java.util.Date
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class DateToXConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            DateToStringConverter(),
            DateToLongEpochMillisConverter(),
            DateToLongEpochSecondsConverter(),
        ).toConverterTestArgumentsWithType {
            "java.util.Date" to it.targetClass.qualifiedName
        }

        private val dateToXConverterClasses: Set<Class<out DateToXConverter>> =
            Reflections(DateToXConverter::class.java)
                .getSubTypesOf(DateToXConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(
            converter = dateToXConverterClasses.newConverterInstance(simpleConverterName),
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName
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
        val epochMillis = 1674060174913
        val sourceInstance = sourceKClass.constructors.first().call(Date(epochMillis))

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
        when {
            targetTypeName.startsWith("kotlin.String") -> {
                targetValue as String
                assertEquals("2023-01-18T16:42:54.913Z", targetValue)
            }

            targetTypeName.startsWith("kotlin.Long") -> {
                targetValue as Long
                when (converter) {
                    is DateToLongEpochMillisConverter -> assertEquals(epochMillis, targetValue)
                    is DateToLongEpochSecondsConverter -> assertEquals(epochMillis / 1000, targetValue)
                }
            }
        }
    }

}

