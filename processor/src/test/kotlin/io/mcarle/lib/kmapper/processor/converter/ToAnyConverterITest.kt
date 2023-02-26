package io.mcarle.lib.kmapper.processor.converter

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class ToAnyConverterITest : ConverterITest() {

    companion object {

        @JvmStatic
        fun sourceAndTargets(): List<Arguments> = listOf(
            "String",
            "Int",
            "UInt",
            "Long",
            "ULong",
            "Short",
            "UShort",
            "Float",
            "Double",
            "Byte",
            "UByte",
            "Char",
            "Boolean",
            "java.util.Date",
            "Any",
            "List<String>",
        )
            .flatMap {
                listOf(
                    Arguments.arguments(it, "Any"),
//                    Arguments.arguments("$it?", "Any"),
                    Arguments.arguments(it, "Any?"),
                    Arguments.arguments("$it?", "Any?"),
                )
            }

    }


    @ParameterizedTest
    @MethodSource("sourceAndTargets")
    fun converterTest(sourceTypeName: String, targetTypeName: String) {
        super.converterTest(ToAnyConverter(), sourceTypeName, targetTypeName)
    }

    override fun verifyMapper(
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
        val sourceValue = when {
            sourceTypeName.startsWith("String") -> "Test"
            sourceTypeName.startsWith("Int") -> -123
            sourceTypeName.startsWith("UInt") -> 123.toUInt()
            sourceTypeName.startsWith("Long") -> -333L
            sourceTypeName.startsWith("ULong") -> 555.toULong()
            sourceTypeName.startsWith("Short") -> 123.toShort()
            sourceTypeName.startsWith("UShort") -> (-123).toUShort()
            sourceTypeName.startsWith("Float") -> 12.34f
            sourceTypeName.startsWith("Double") -> 3.141
            sourceTypeName.startsWith("Byte") -> (-123).toByte()
            sourceTypeName.startsWith("UByte") -> 123.toUByte()
            sourceTypeName.startsWith("Char") -> 'M'
            sourceTypeName.startsWith("Boolean") -> true
            sourceTypeName.startsWith("java.util.Date") -> Date()
            sourceTypeName.startsWith("Any") -> mapOf("Hallo" to "Welt")
            sourceTypeName.startsWith("List<String>") -> listOf("Hallo", "Welt")
            else -> null
        }
        val sourceInstance = sourceKClass.constructors.first().call(sourceValue)

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
        when {
            sourceTypeName.endsWith("?")
                    xor targetTypeName.endsWith("?")
                    || listOf("String", "List", "java", "Any").none { sourceTypeName.startsWith(it) } ->
                Assertions.assertEquals(
                    sourceValue,
                    targetValue
                )

            else -> Assertions.assertSame(
                sourceValue,
                targetValue
            )
        }
    }

}

