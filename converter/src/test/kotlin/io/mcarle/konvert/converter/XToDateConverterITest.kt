package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

class XToDateConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            StringToDateConverter(),
            LongEpochMillisToDateConverter(),
            LongEpochSecondsToDateConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to "java.util.Date"
        }

        private val xToDateConverterClasses: Set<Class<out XToDateConverter>> = Reflections(XToDateConverter::class.java)
            .getSubTypesOf(XToDateConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(xToDateConverterClasses.newConverterInstance(simpleConverterName), sourceTypeName, targetTypeName)
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
        val instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val sourceInstance = sourceKClass.constructors.first().call(
            when {
                sourceTypeName.startsWith("kotlin.String") -> instant.toString()
                sourceTypeName.startsWith("kotlin.Long") -> when (converter) {
                    is LongEpochMillisToDateConverter -> instant.toEpochMilli()
                    is LongEpochSecondsToDateConverter -> instant.toEpochMilli() / 1000
                    else -> null
                }

                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
        assertEquals(Date(instant.toEpochMilli()), targetValue)
    }

}

