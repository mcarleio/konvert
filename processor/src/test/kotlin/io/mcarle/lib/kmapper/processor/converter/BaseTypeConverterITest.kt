package io.mcarle.lib.kmapper.processor.converter

import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.kotlin.util.suffixIfNot
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.paukov.combinatorics3.Generator
import org.reflections.Reflections
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class BaseTypeConverterITest : ConverterITest() {

    companion object {

        @JvmStatic
        fun cartesianProductOfTypes(): List<Arguments> {
            val types = mutableListOf(
                "String",
                "Int",
                "UInt",
                "Long",
                "ULong",
                "Short",
                "UShort",
                "Number",
                "Byte",
                "UByte",
                "Char",
                "Boolean",
                "Float",
                "Double",
            ).flatMap { listOf(it, "$it?") }

            return Generator.cartesianProduct(types, types)
                .stream()
                .filter { it[0].suffixIfNot("?") != it[1].suffixIfNot("?") }
                .map { arguments(it[0], it[1]) }
                .toList()
        }

        private val baseTypeConverterClasses: Set<Class<out BaseTypeConverter>> = Reflections(BaseTypeConverter::class.java)
            .getSubTypesOf(BaseTypeConverter::class.java)

    }


    @ParameterizedTest
    @MethodSource("cartesianProductOfTypes")
    fun converterTest(sourceTypeName: String, targetTypeName: String) {
        super.converterTest(
            converter = determineConverter(
                sourceTypeName.removeSuffixIfPresent("?"),
                targetTypeName.removeSuffixIfPresent("?")
            ),
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName
        )
    }

    private fun determineConverter(sourceTypeName: String, targetTypeName: String): BaseTypeConverter {
        return baseTypeConverterClasses
            .firstNotNullOf {
                val converter = it.getDeclaredConstructor().newInstance()
                if (converter.sourceClass.simpleName == sourceTypeName &&
                    converter.targetClass.simpleName == targetTypeName
                ) {
                    converter
                } else {
                    null
                }
            }
    }

    override fun verifyMapper(
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
        val sourceInstance = sourceKClass.constructors.first().call(
            when {
                sourceTypeName.startsWith("String") -> "1"
                sourceTypeName.startsWith("Int") -> -888
                sourceTypeName.startsWith("UInt") -> 777u
                sourceTypeName.startsWith("Long") -> -9999L
                sourceTypeName.startsWith("ULong") -> 6666.toULong()
                sourceTypeName.startsWith("Short") -> (-512).toShort()
                sourceTypeName.startsWith("UShort") -> 512.toUShort()
                sourceTypeName.startsWith("Number") -> 1423847
                sourceTypeName.startsWith("Byte") -> (-1).toByte()
                sourceTypeName.startsWith("UByte") -> 128.toUByte()
                sourceTypeName.startsWith("Char") -> 'A'
                sourceTypeName.startsWith("Boolean") -> true
                sourceTypeName.startsWith("Float") -> 3.141f
                sourceTypeName.startsWith("Double") -> 1337.1337
                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        assertDoesNotThrow {
            val targetValue = targetKClass.members.first { it.name == "test" }.call(targetInstance)
            when {
                targetTypeName.startsWith("String") -> targetValue as String
                targetTypeName.startsWith("Int") -> targetValue as Int
                targetTypeName.startsWith("UInt") -> targetValue as UInt
                targetTypeName.startsWith("Long") -> targetValue as Long
                targetTypeName.startsWith("ULong") -> targetValue as ULong
                targetTypeName.startsWith("Short") -> targetValue as Short
                targetTypeName.startsWith("UShort") -> targetValue as UShort
                targetTypeName.startsWith("Number") -> targetValue as Number
                targetTypeName.startsWith("Byte") -> targetValue as Byte
                targetTypeName.startsWith("UByte") -> targetValue as UByte
                targetTypeName.startsWith("Char") -> targetValue as Char
                targetTypeName.startsWith("Boolean") -> targetValue as Boolean
                targetTypeName.startsWith("Float") -> targetValue as Float
                targetTypeName.startsWith("Double") -> targetValue as Double
                else -> null
            }
        }
    }

}

