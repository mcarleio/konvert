package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import java.math.BigDecimal
import java.math.BigInteger
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
            StringToBigIntegerConverter(),
            IntToBigIntegerConverter(),
            UIntToBigIntegerConverter(),
            LongToBigIntegerConverter(),
            ULongToBigIntegerConverter(),
            ShortToBigIntegerConverter(),
            UShortToBigIntegerConverter(),
            FloatToBigIntegerConverter(),
            DoubleToBigIntegerConverter(),
            ByteToBigIntegerConverter(),
            UByteToBigIntegerConverter(),
            NumberToBigIntegerConverter(),
            CharToBigIntegerConverter(),
            BooleanToBigIntegerConverter(),
            BigIntegerToStringConverter(),
            BigIntegerToIntConverter(),
            BigIntegerToUIntConverter(),
            BigIntegerToLongConverter(),
            BigIntegerToULongConverter(),
            BigIntegerToShortConverter(),
            BigIntegerToUShortConverter(),
            BigIntegerToByteConverter(),
            BigIntegerToUByteConverter(),
            BigIntegerToCharConverter(),
            BigIntegerToBooleanConverter(),
            BigIntegerToFloatConverter(),
            BigIntegerToDoubleConverter(),
            BigIntegerToNumberConverter(),
            BigIntegerToBigDecimalConverter(),
            BigDecimalToBigIntegerConverter(),
            StringToBigDecimalConverter(),
            IntToBigDecimalConverter(),
            UIntToBigDecimalConverter(),
            LongToBigDecimalConverter(),
            ULongToBigDecimalConverter(),
            ShortToBigDecimalConverter(),
            UShortToBigDecimalConverter(),
            FloatToBigDecimalConverter(),
            DoubleToBigDecimalConverter(),
            ByteToBigDecimalConverter(),
            UByteToBigDecimalConverter(),
            NumberToBigDecimalConverter(),
            CharToBigDecimalConverter(),
            BooleanToBigDecimalConverter(),
            BigDecimalToStringConverter(),
            BigDecimalToIntConverter(),
            BigDecimalToUIntConverter(),
            BigDecimalToLongConverter(),
            BigDecimalToULongConverter(),
            BigDecimalToShortConverter(),
            BigDecimalToUShortConverter(),
            BigDecimalToByteConverter(),
            BigDecimalToUByteConverter(),
            BigDecimalToCharConverter(),
            BigDecimalToBooleanConverter(),
            BigDecimalToFloatConverter(),
            BigDecimalToDoubleConverter(),
            BigDecimalToNumberConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to it.targetClass.qualifiedName
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
            "kotlin.Int",
            "kotlin.String"
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
                sourceTypeName.startsWith("java.math.BigInteger") -> BigInteger.ONE
                sourceTypeName.startsWith("java.math.BigDecimal") -> BigDecimal.ONE
                sourceTypeName.startsWith("kotlin.String") -> "1"
                sourceTypeName.startsWith("kotlin.Int") -> -888
                sourceTypeName.startsWith("kotlin.UInt") -> 777u
                sourceTypeName.startsWith("kotlin.Long") -> -9999L
                sourceTypeName.startsWith("kotlin.ULong") -> 6666.toULong()
                sourceTypeName.startsWith("kotlin.Short") -> (-512).toShort()
                sourceTypeName.startsWith("kotlin.UShort") -> 512.toUShort()
                sourceTypeName.startsWith("kotlin.Number") -> 1423847
                sourceTypeName.startsWith("kotlin.Byte") -> (-1).toByte()
                sourceTypeName.startsWith("kotlin.UByte") -> 128.toUByte()
                sourceTypeName.startsWith("kotlin.Char") -> 'A'
                sourceTypeName.startsWith("kotlin.Boolean") -> true
                sourceTypeName.startsWith("kotlin.Float") -> 3.141f
                sourceTypeName.startsWith("kotlin.Double") -> 1337.1337
                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        assertDoesNotThrow {
            val targetValue = targetKClass.members.first { it.name == "test" }.call(targetInstance)
            when {
                targetTypeName.startsWith("kotlin.String") -> targetValue as String
                targetTypeName.startsWith("kotlin.Int") -> targetValue as Int
                targetTypeName.startsWith("kotlin.UInt") -> targetValue as UInt
                targetTypeName.startsWith("kotlin.Long") -> targetValue as Long
                targetTypeName.startsWith("kotlin.ULong") -> targetValue as ULong
                targetTypeName.startsWith("kotlin.Short") -> targetValue as Short
                targetTypeName.startsWith("kotlin.UShort") -> targetValue as UShort
                targetTypeName.startsWith("kotlin.Number") -> targetValue as Number
                targetTypeName.startsWith("kotlin.Byte") -> targetValue as Byte
                targetTypeName.startsWith("kotlin.UByte") -> targetValue as UByte
                targetTypeName.startsWith("kotlin.Char") -> targetValue as Char
                targetTypeName.startsWith("kotlin.Boolean") -> targetValue as Boolean
                targetTypeName.startsWith("kotlin.Float") -> targetValue as Float
                targetTypeName.startsWith("kotlin.Double") -> targetValue as Double
                targetTypeName.startsWith("java.math.BigInteger") -> targetValue as BigInteger
                targetTypeName.startsWith("java.math.BigDecimal") -> targetValue as BigDecimal
                else -> null
            }
        }
    }

}

