package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.SAME_TYPE_PRIORITY
import io.mcarle.konvert.converter.api.TypeConverter
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
class ArrayToArrayConverterITest : ConverterITest() {

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
        ).toConverterTestArguments { "Array<Array<$it>>" to "Array<${it}Array>" }

        @JvmStatic
        fun mix(): List<Arguments> = listOf(
            "Int",
        ).toConverterTestArguments { "Array<Short>" to "${it}Array" }

        @JvmStatic
        fun primitiveToPrimitiveTypes(): List<Arguments> = listOf(
            "DoubleArray",
            "FloatArray",
            "LongArray",
            "IntArray",
            "CharArray",
            "ShortArray",
            "ByteArray",
            "BooleanArray",
        ).toConverterTestArguments { it to it }

        @JvmStatic
        fun primitiveToNonPrimitiveTypes(): List<Arguments> = listOf(
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
                Arguments.arguments("${it}Array", "Array<$it>"),
                Arguments.arguments("${it}Array", "Array<$it?>"),
                Arguments.arguments("${it}Array", "Array<$it>?")
            )
        }

        @JvmStatic
        fun nonPrimitiveToPrimitiveTypes(): List<Arguments> = listOf(
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
                Arguments.arguments("Array<$it>?", "${it}Array?"),
                Arguments.arguments("Array<$it>", "${it}Array")
            )
        }

        @JvmStatic
        fun nullableCombinations(): List<Arguments> = listOf(
            Arguments.arguments("Array<Int>", "Array<Int>"),
            Arguments.arguments("Array<Int>", "Array<Int?>"),
            Arguments.arguments("Array<Int?>", "Array<Int?>"),
            Arguments.arguments("Array<Int>?", "Array<Int>?"),
            Arguments.arguments("Array<Int>?", "Array<Int?>?"),
            Arguments.arguments("Array<Int?>?", "Array<Int?>?"),
            Arguments.arguments("Array<Int>", "Array<Int>?"),
            Arguments.arguments("Array<Int>", "Array<Int?>?"),
            Arguments.arguments("Array<Int?>", "Array<Int?>?"),
        )

    }

    @ParameterizedTest
    @MethodSource("types")
    fun converterTest(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = "Array<$sourceTypeName>",
            targetTypeName = "Array<$targetTypeName>",
            converter = ArrayToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter())
        )
    }

    @ParameterizedTest
    @MethodSource("combo")
    fun combo(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = ArrayToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter())
        )
    }

    @ParameterizedTest
    @MethodSource("mix")
    fun mix(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = ArrayToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter(), ShortToIntConverter())
        )
    }

    @ParameterizedTest
    @MethodSource("primitiveToPrimitiveTypes")
    fun primitiveTest(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = object : TypeConverter by ArrayToArrayConverter() {
                override val priority = SAME_TYPE_PRIORITY - 1 // ensure higher priority than SameTypeConverter
            },
            additionalConverter = arrayOf(SameTypeConverter())
        )
    }

    @ParameterizedTest
    @MethodSource("primitiveToNonPrimitiveTypes")
    fun primitiveToNonPrimitiveTest(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = ArrayToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter())
        )
    }

    @ParameterizedTest
    @MethodSource("nonPrimitiveToPrimitiveTypes")
    fun nonPrimitiveToPrimitiveTest(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = ArrayToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter())
        )
    }

    @ParameterizedTest
    @MethodSource("nullableCombinations")
    fun nullableCombinationsTest(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = ArrayToArrayConverter(),
            additionalConverter = arrayOf(SameTypeConverter())
        )
    }

    override fun verify(verificationData: VerificationData) {
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceTypeName = sourceVariable.second
            val genericTypeName = sourceTypeName.substringAfter("<").removeSuffix(">").trim()
            when {
                sourceTypeName.startsWith("BooleanArray") -> booleanArrayOf(false)
                sourceTypeName.startsWith("DoubleArray") -> doubleArrayOf(1.0)
                sourceTypeName.startsWith("FloatArray") -> floatArrayOf(2.0f)
                sourceTypeName.startsWith("LongArray") -> longArrayOf(3)
                sourceTypeName.startsWith("IntArray") -> intArrayOf(4)
                sourceTypeName.startsWith("ShortArray") -> shortArrayOf(5)
                sourceTypeName.startsWith("ByteArray") -> byteArrayOf(6)
                sourceTypeName.startsWith("CharArray") -> charArrayOf('A')

                genericTypeName.startsWith("IntArray") -> arrayOf(intArrayOf(4))
                genericTypeName.startsWith("ByteArray") -> arrayOf(byteArrayOf(6))

                genericTypeName.startsWith("Boolean") -> arrayOf(false)
                genericTypeName.startsWith("Double") -> arrayOf(1.0)
                genericTypeName.startsWith("Float") -> arrayOf(2.0f)
                genericTypeName.startsWith("Long") -> arrayOf(3L)
                genericTypeName.startsWith("Int") -> arrayOf(4)
                genericTypeName.startsWith("Short") -> arrayOf(5.toShort())
                genericTypeName.startsWith("Byte") -> arrayOf(6.toByte())
                genericTypeName.startsWith("Char") -> arrayOf('A')

                genericTypeName.startsWith("String") -> arrayOf("888")
                genericTypeName.startsWith("List<String>") -> arrayOf(listOf("666"))
                genericTypeName.startsWith("Array<Int>") -> arrayOf(arrayOf(123))
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
    fun variance() {
        val (compilation) = compileWith(
            enabledConverters = listOf(ArrayToArrayConverter(), SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetInVarianceClass::class)
@KonvertTo(TargetOutVarianceClass::class)
@KonvertTo(TargetNoVarianceClass::class)
class SourceClass(val property: Array<out Int>)

class TargetInVarianceClass(val property: Array<in Int>)
class TargetOutVarianceClass(val property: Array<out Int>)
class TargetNoVarianceClass(val property: Array<Int>)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")

        assertSourceEquals(
            """
public fun SourceClass.toTargetInVarianceClass(): TargetInVarianceClass = TargetInVarianceClass(
  property = property.map { it }.toTypedArray()
)

public fun SourceClass.toTargetOutVarianceClass(): TargetOutVarianceClass = TargetOutVarianceClass(
  property = property
)

public fun SourceClass.toTargetNoVarianceClass(): TargetNoVarianceClass = TargetNoVarianceClass(
  property = property.map { it }.toTypedArray()
)
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun sameType() {
        val (compilation) = compileWith(
            enabledConverters = listOf(ArrayToArrayConverter(), SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(val property: Array<Int>)
class TargetClass(val property: Array<Int>)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")

        assertSourceEquals(
            """
public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
  property = property
)
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}
