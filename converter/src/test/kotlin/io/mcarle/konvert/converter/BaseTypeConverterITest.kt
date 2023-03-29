package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class BaseTypeConverterITest : ConverterITest() {

    companion object {

        @JvmStatic
        fun converterList() = listOf(
            StringToIntConverter(),
            StringToUIntConverter(),
            StringToLongConverter(),
            StringToULongConverter(),
            StringToShortConverter(),
            StringToUShortConverter(),
            StringToNumberConverter(),
            StringToByteConverter(),
            StringToUByteConverter(),
            StringToCharConverter(),
            StringToBooleanConverter(),
            StringToFloatConverter(),
            StringToDoubleConverter(),
            IntToStringConverter(),
            IntToUIntConverter(),
            IntToLongConverter(),
            IntToULongConverter(),
            IntToShortConverter(),
            IntToUShortConverter(),
            IntToNumberConverter(),
            IntToByteConverter(),
            IntToUByteConverter(),
            IntToCharConverter(),
            IntToBooleanConverter(),
            IntToFloatConverter(),
            IntToDoubleConverter(),
            UIntToStringConverter(),
            UIntToIntConverter(),
            UIntToLongConverter(),
            UIntToULongConverter(),
            UIntToShortConverter(),
            UIntToUShortConverter(),
            UIntToNumberConverter(),
            UIntToByteConverter(),
            UIntToUByteConverter(),
            UIntToCharConverter(),
            UIntToBooleanConverter(),
            UIntToFloatConverter(),
            UIntToDoubleConverter(),
            LongToStringConverter(),
            LongToIntConverter(),
            LongToUIntConverter(),
            LongToULongConverter(),
            LongToShortConverter(),
            LongToUShortConverter(),
            LongToNumberConverter(),
            LongToByteConverter(),
            LongToUByteConverter(),
            LongToCharConverter(),
            LongToBooleanConverter(),
            LongToFloatConverter(),
            LongToDoubleConverter(),
            ULongToStringConverter(),
            ULongToIntConverter(),
            ULongToUIntConverter(),
            ULongToLongConverter(),
            ULongToShortConverter(),
            ULongToUShortConverter(),
            ULongToNumberConverter(),
            ULongToByteConverter(),
            ULongToUByteConverter(),
            ULongToCharConverter(),
            ULongToBooleanConverter(),
            ULongToFloatConverter(),
            ULongToDoubleConverter(),
            ShortToStringConverter(),
            ShortToIntConverter(),
            ShortToUIntConverter(),
            ShortToLongConverter(),
            ShortToULongConverter(),
            ShortToUShortConverter(),
            ShortToNumberConverter(),
            ShortToByteConverter(),
            ShortToUByteConverter(),
            ShortToCharConverter(),
            ShortToBooleanConverter(),
            ShortToFloatConverter(),
            ShortToDoubleConverter(),
            UShortToStringConverter(),
            UShortToIntConverter(),
            UShortToUIntConverter(),
            UShortToLongConverter(),
            UShortToULongConverter(),
            UShortToShortConverter(),
            UShortToNumberConverter(),
            UShortToByteConverter(),
            UShortToUByteConverter(),
            UShortToCharConverter(),
            UShortToBooleanConverter(),
            UShortToFloatConverter(),
            UShortToDoubleConverter(),
            FloatToStringConverter(),
            FloatToIntConverter(),
            FloatToUIntConverter(),
            FloatToLongConverter(),
            FloatToULongConverter(),
            FloatToShortConverter(),
            FloatToUShortConverter(),
            FloatToNumberConverter(),
            FloatToByteConverter(),
            FloatToUByteConverter(),
            FloatToCharConverter(),
            FloatToBooleanConverter(),
            FloatToDoubleConverter(),
            DoubleToStringConverter(),
            DoubleToIntConverter(),
            DoubleToUIntConverter(),
            DoubleToLongConverter(),
            DoubleToULongConverter(),
            DoubleToShortConverter(),
            DoubleToUShortConverter(),
            DoubleToNumberConverter(),
            DoubleToByteConverter(),
            DoubleToUByteConverter(),
            DoubleToCharConverter(),
            DoubleToBooleanConverter(),
            DoubleToFloatConverter(),
            ByteToStringConverter(),
            ByteToIntConverter(),
            ByteToUIntConverter(),
            ByteToLongConverter(),
            ByteToULongConverter(),
            ByteToShortConverter(),
            ByteToUShortConverter(),
            ByteToNumberConverter(),
            ByteToUByteConverter(),
            ByteToDoubleConverter(),
            ByteToCharConverter(),
            ByteToBooleanConverter(),
            ByteToFloatConverter(),
            UByteToStringConverter(),
            UByteToIntConverter(),
            UByteToUIntConverter(),
            UByteToLongConverter(),
            UByteToULongConverter(),
            UByteToShortConverter(),
            UByteToUShortConverter(),
            UByteToNumberConverter(),
            UByteToByteConverter(),
            UByteToCharConverter(),
            UByteToBooleanConverter(),
            UByteToFloatConverter(),
            UByteToDoubleConverter(),
            NumberToStringConverter(),
            NumberToIntConverter(),
            NumberToUIntConverter(),
            NumberToLongConverter(),
            NumberToULongConverter(),
            NumberToShortConverter(),
            NumberToUShortConverter(),
            NumberToByteConverter(),
            NumberToUByteConverter(),
            NumberToCharConverter(),
            NumberToBooleanConverter(),
            NumberToFloatConverter(),
            NumberToDoubleConverter(),
            CharToStringConverter(),
            CharToIntConverter(),
            CharToUIntConverter(),
            CharToLongConverter(),
            CharToULongConverter(),
            CharToShortConverter(),
            CharToUShortConverter(),
            CharToNumberConverter(),
            CharToDoubleConverter(),
            CharToByteConverter(),
            CharToUByteConverter(),
            CharToBooleanConverter(),
            CharToFloatConverter(),
            BooleanToStringConverter(),
            BooleanToIntConverter(),
            BooleanToUIntConverter(),
            BooleanToLongConverter(),
            BooleanToULongConverter(),
            BooleanToShortConverter(),
            BooleanToUShortConverter(),
            BooleanToNumberConverter(),
            BooleanToDoubleConverter(),
            BooleanToByteConverter(),
            BooleanToUByteConverter(),
            BooleanToCharConverter(),
            BooleanToFloatConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.simpleName to it.targetClass.simpleName
        }

        private val baseTypeConverterClasses: Set<Class<out BaseTypeConverter>> =
            Reflections(BaseTypeConverter::class.java)
                .getSubTypesOf(BaseTypeConverter::class.java)

    }

    @Tag("detailed")
    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(
            baseTypeConverterClasses.newConverterInstance(simpleConverterName),
            sourceTypeName,
            targetTypeName
        )
    }

    @Test
    fun simple() {
        super.converterTest(
            IntToStringConverter(),
            "Int",
            "String"
        )
    }

    override fun verifyMapper(
        converter: TypeConverter,
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

