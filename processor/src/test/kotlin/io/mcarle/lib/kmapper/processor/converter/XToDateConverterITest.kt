package io.mcarle.lib.kmapper.processor.converter

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

class XToDateConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            StringToDateConverter(),
            LongToDateConverter(),
        ).toConverterTestArguments {
            it.sourceClass.qualifiedName to "java.util.Date"
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
                sourceTypeName.startsWith("kotlin.String") -> instant.toString()
                sourceTypeName.startsWith("kotlin.Long") -> instant.toEpochMilli()
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

