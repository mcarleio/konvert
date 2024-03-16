package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class XToEnumConverterITest : ConverterITest() {

    companion object {

        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            StringToEnumConverter(),
            IntToEnumConverter(),
            UIntToEnumConverter(),
            LongToEnumConverter(),
            ULongToEnumConverter(),
            ShortToEnumConverter(),
            UShortToEnumConverter(),
            NumberToEnumConverter(),
            DoubleToEnumConverter(),
            ByteToEnumConverter(),
            UByteToEnumConverter(),
            FloatToEnumConverter(),
            BigIntegerToEnumConverter(),
            BigDecimalToEnumConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to "MyEnum"
        }

        private val xToEnumConverterClasses: Set<Class<out XToEnumConverter>> = Reflections(XToEnumConverter::class.java)
            .getSubTypesOf(XToEnumConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            xToEnumConverterClasses.newConverterInstance(simpleConverterName),
            additionalCode = this.generateAdditionalCode()
        )
    }

    private fun generateAdditionalCode(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            name = "MyEnum.kt",
            contents =
            """
enum class MyEnum {
    XXX,
    YYY,
    ZZZ
}
           """.trimIndent()
        )
    )

    override fun verify(verificationData: VerificationData) {
        val value = (0..2).random()
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceTypeName = sourceVariable.second
            when {
                sourceTypeName.startsWith("kotlin.String") -> when (value) {
                    0 -> "XXX"
                    1 -> "YYY"
                    else -> "ZZZ"
                }
                sourceTypeName.startsWith("java.math.BigInteger") -> value.toBigInteger()
                sourceTypeName.startsWith("java.math.BigDecimal") -> value.toBigDecimal()
                sourceTypeName.startsWith("kotlin.Int") -> value
                sourceTypeName.startsWith("kotlin.UInt") -> value.toUInt()
                sourceTypeName.startsWith("kotlin.Long") -> value.toLong()
                sourceTypeName.startsWith("kotlin.ULong") -> value.toULong()
                sourceTypeName.startsWith("kotlin.Short") -> value.toShort()
                sourceTypeName.startsWith("kotlin.UShort") -> value.toUShort()
                sourceTypeName.startsWith("kotlin.Number") -> value
                sourceTypeName.startsWith("kotlin.Byte") -> value.toByte()
                sourceTypeName.startsWith("kotlin.UByte") -> value.toUByte()
                sourceTypeName.startsWith("kotlin.Float") -> value.toFloat()
                sourceTypeName.startsWith("kotlin.Double") -> value.toDouble()
                else -> null
            }
        }
        val sourceInstance = verificationData.sourceKClass.constructors.first().call(*sourceValues.toTypedArray())

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEach { targetVariable ->
            val targetName = targetVariable.first
            val targetValue = assertDoesNotThrow {
                verificationData.targetKClass.members.first { it.name == targetName }.call(targetInstance) as Enum<*>
            }
            assertEquals(value, targetValue.ordinal)
        }

    }

}

