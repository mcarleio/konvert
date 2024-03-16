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
import kotlin.reflect.KClass
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class EnumToXConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            EnumToStringConverter(),
            EnumToIntConverter(),
            EnumToUIntConverter(),
            EnumToLongConverter(),
            EnumToULongConverter(),
            EnumToShortConverter(),
            EnumToUShortConverter(),
            EnumToNumberConverter(),
            EnumToDoubleConverter(),
            EnumToByteConverter(),
            EnumToUByteConverter(),
            EnumToCharConverter(),
            EnumToFloatConverter(),
            EnumToBigIntegerConverter(),
            EnumToBigDecimalConverter(),
        ).toConverterTestArgumentsWithType {
            "MyEnum" to it.targetClass.qualifiedName
        }

        private val enumToXConverterClasses: Set<Class<out EnumToXConverter>> = Reflections(EnumToXConverter::class.java)
            .getSubTypesOf(EnumToXConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = enumToXConverterClasses.newConverterInstance(simpleConverterName),
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
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceVariableName = sourceVariable.first
            (verificationData.sourceKClass.members.first { it.name == sourceVariableName }.returnType.classifier as KClass<*>).java
                .enumConstants.random() as Enum<*>
        }
        val sourceInstance = verificationData.sourceKClass.constructors.first().call(*sourceValues.toTypedArray())

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEachIndexed { index, targetVariable ->
            val targetVariableName = targetVariable.first
            val targetTypeName = targetVariable.second
            val targetValue = assertDoesNotThrow {
                verificationData.targetKClass.members.first { it.name == targetVariableName }.call(targetInstance)
            }
            val enumValue = sourceValues[index]
            assertEquals(
                when {
                    targetTypeName.startsWith("java.math.BigInteger") -> enumValue.ordinal.toBigInteger()
                    targetTypeName.startsWith("java.math.BigDecimal") -> enumValue.ordinal.toBigDecimal()
                    targetTypeName.startsWith("kotlin.String") -> enumValue.name
                    targetTypeName.startsWith("kotlin.Int") -> enumValue.ordinal
                    targetTypeName.startsWith("kotlin.UInt") -> enumValue.ordinal.toUInt()
                    targetTypeName.startsWith("kotlin.Long") -> enumValue.ordinal.toLong()
                    targetTypeName.startsWith("kotlin.ULong") -> enumValue.ordinal.toULong()
                    targetTypeName.startsWith("kotlin.Short") -> enumValue.ordinal.toShort()
                    targetTypeName.startsWith("kotlin.UShort") -> enumValue.ordinal.toUShort()
                    targetTypeName.startsWith("kotlin.Number") -> enumValue.ordinal
                    targetTypeName.startsWith("kotlin.Double") -> enumValue.ordinal.toDouble()
                    targetTypeName.startsWith("kotlin.Byte") -> enumValue.ordinal.toByte()
                    targetTypeName.startsWith("kotlin.UByte") -> enumValue.ordinal.toUByte()
                    targetTypeName.startsWith("kotlin.Char") -> enumValue.ordinal.toChar()
                    targetTypeName.startsWith("kotlin.Float") -> enumValue.ordinal.toFloat()
                    else -> null
                },
                targetValue
            )
        }
    }

}

