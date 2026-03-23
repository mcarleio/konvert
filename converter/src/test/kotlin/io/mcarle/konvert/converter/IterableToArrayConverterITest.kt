package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource


@OptIn(ExperimentalCompilerApi::class)
class IterableToArrayConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun types(): List<Arguments> = listOf(
            "String",
            "Int",
            "List<String>",
            "Array<Int>",
            "IntArray",
            "ByteArray",
        ).toConverterTestArguments { it to it }

        @JvmStatic
        fun combo(): List<Arguments> = listOf(
            "Int",
        ).toConverterTestArguments { "List<List<$it>>" to "Array<${it}Array>" }

        @JvmStatic
        fun mix(): List<Arguments> = listOf(
            "Int",
        ).toConverterTestArguments { "List<Short>" to "${it}Array" }

        @JvmStatic
        fun toPrimitiveTypes(): List<Arguments> = listOf(
            "Double",
            "Float",
            "Long",
            "Int",
            "Char",
            "Short",
            "Byte",
            "Boolean",
        ).flatMap {
            listOf(
                Arguments.arguments("List<$it>?", "${it}Array?"),
                Arguments.arguments("List<$it>", "${it}Array")
            )
        }

    }

    @ParameterizedTest
    @MethodSource("types")
    fun converterTest(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = "List<$sourceTypeName>",
            targetTypeName = "Array<$targetTypeName>",
            converter = IterableToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter())
        )
    }

    @ParameterizedTest
    @MethodSource("combo")
    fun combo(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = IterableToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter())
        )
    }

    @ParameterizedTest
    @MethodSource("mix")
    fun mix(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = IterableToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter(), ShortToIntConverter())
        )
    }

    @ParameterizedTest
    @MethodSource("toPrimitiveTypes")
    fun toPrimitiveTest(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = IterableToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter())
        )
    }

    override fun verify(verificationData: VerificationData) {
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceTypeName = sourceVariable.second
            val genericTypeName = sourceTypeName.substringAfter("<").removeSuffix(">").trim()
            when {
                genericTypeName.startsWith("IntArray") -> listOf(intArrayOf(4))
                genericTypeName.startsWith("ByteArray") -> listOf(byteArrayOf(6))

                genericTypeName.startsWith("Boolean") -> listOf(false)
                genericTypeName.startsWith("Double") -> listOf(1.0)
                genericTypeName.startsWith("Float") -> listOf(2.0f)
                genericTypeName.startsWith("Long") -> listOf(3L)
                genericTypeName.startsWith("Int") -> listOf(4)
                genericTypeName.startsWith("Short") -> listOf(5.toShort())
                genericTypeName.startsWith("Byte") -> listOf(6.toByte())
                genericTypeName.startsWith("Char") -> listOf('A')

                genericTypeName.startsWith("String") -> listOf("888")
                genericTypeName.startsWith("List<String>") -> listOf(listOf("666"))
                genericTypeName.startsWith("List<Int>") -> listOf(listOf(444))
                genericTypeName.startsWith("Array<Int>") -> listOf(arrayOf(123))
                else -> null
            }
        }

        val sourceInstance = verificationData.sourceKClass.constructors.first().call(sourceValues[0])

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEach { targetVariable ->
            val targetName = targetVariable.first

            assertDoesNotThrow {
                val result = verificationData.targetKClass.members.first { it.name == targetName }.call(targetInstance)

                when {
                    targetVariable.second.startsWith("BooleanArray") -> result as BooleanArray
                    targetVariable.second.startsWith("DoubleArray") -> result as DoubleArray
                    targetVariable.second.startsWith("FloatArray") -> result as FloatArray
                    targetVariable.second.startsWith("LongArray") -> result as LongArray
                    targetVariable.second.startsWith("IntArray") -> result as IntArray
                    targetVariable.second.startsWith("ShortArray") -> result as ShortArray
                    targetVariable.second.startsWith("ByteArray") -> result as ByteArray
                    targetVariable.second.startsWith("CharArray") -> result as CharArray
                    else -> result as Array<*>
                }
            }
        }
    }

    @Test
    fun sameType() {
        val (compilation) = compileWith(
            enabledConverters = listOf(IterableToArrayConverter(), SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(val property: Set<Int>)
class TargetClass(val property: Array<Int>)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")

        assertSourceEquals(
            """
public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
  property = property.toTypedArray()
)
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}
