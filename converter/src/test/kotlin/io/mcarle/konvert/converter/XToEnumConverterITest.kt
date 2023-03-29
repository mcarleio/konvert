package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
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
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to "MyEnum"
        }

        private val xToEnumConverterClasses: Set<Class<out XToEnumConverter>> = Reflections(XToEnumConverter::class.java)
            .getSubTypesOf(XToEnumConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(
            converter = xToEnumConverterClasses.newConverterInstance(simpleConverterName),
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName
        )
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
        converter: TypeConverter,
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

