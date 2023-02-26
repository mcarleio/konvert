package io.mcarle.lib.kmapper.processor.converter

import io.mcarle.lib.kmapper.processor.TypeConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class DateToXConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            DateToStringConverter(),
            DateToLongConverter(),
            DateToInstantConverter(),
        ).toConverterTestArguments {
            "java.util.Date" to it.targetClass.qualifiedName
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
                assertEquals(epochMillis, targetValue)
            }

            targetTypeName.startsWith("java.time.Instant") -> {
                targetValue as Instant
                assertEquals(epochMillis, targetValue.toEpochMilli())
            }

            else -> null
        }
    }

}

