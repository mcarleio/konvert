package io.mcarle.lib.kmapper.processor.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.lib.kmapper.processor.TypeConverter
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

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
        ).toConverterTestArguments {
            it.sourceClass.qualifiedName to "MyEnum"
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
        val value = (0..2).random()
        val sourceInstance = sourceKClass.constructors.first().call(
            when {
                sourceTypeName.startsWith("kotlin.String") -> when (value) {
                    0 -> "XXX"
                    1 -> "YYY"
                    else -> "ZZZ"
                }
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
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance) as Enum<*>
        }
        assertEquals(value, targetValue.ordinal)
    }

}

