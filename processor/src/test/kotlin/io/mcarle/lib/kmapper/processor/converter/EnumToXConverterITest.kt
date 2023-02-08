package io.mcarle.lib.kmapper.processor.converter

import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

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
        ).toConverterTestArguments {
            "MyEnum" to it.targetClass.qualifiedName
        }
    }

    @ParameterizedTest()
    @MethodSource("converterList")
    override fun converterTest(converter: TypeConverter, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(converter, sourceTypeName, targetTypeName)
    }

    override fun generateAdditionalCode(): SourceFile = SourceFile.kotlin(
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

    override fun verifyMapper(
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
        val enumValue =
            (sourceKClass.members.first { it.name == "test" }.returnType.classifier as KClass<*>).java.enumConstants.random() as Enum<*>
        val sourceInstance = sourceKClass.constructors.first().call(
            enumValue
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }

        Assertions.assertEquals(
            when {
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

