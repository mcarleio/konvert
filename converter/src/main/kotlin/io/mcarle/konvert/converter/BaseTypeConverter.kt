package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.isNullable
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

abstract class BaseTypeConverter(
    val sourceClass: KClass<*>,
    val targetClass: KClass<*>,
    override val enabledByDefault: Boolean = false
) : AbstractTypeConverter() {

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType()
    }
    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asStarProjectedType()
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceNotNullable == sourceType && targetNotNullable == targetType
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "")

        return CodeBlock.of(
            if (needsNotNullAssertionOperator(source, target)) {
                appendNotNullAssertionOperator(convertCode)
            } else {
                convertCode
            }
        )
    }

    abstract fun convert(fieldName: String, nc: String): String
    protected open fun appendNotNullAssertionOperator(code: String): String = "$code!!"

}

// ================
// ==== STRING ====
// ================

@AutoService(TypeConverter::class)
class StringToIntConverter : BaseTypeConverter(String::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class StringToUIntConverter : BaseTypeConverter(String::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class StringToLongConverter : BaseTypeConverter(String::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class StringToULongConverter : BaseTypeConverter(String::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class StringToShortConverter : BaseTypeConverter(String::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

@AutoService(TypeConverter::class)
class StringToUShortConverter : BaseTypeConverter(String::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class StringToNumberConverter : BaseTypeConverter(String::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class StringToByteConverter : BaseTypeConverter(String::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

@AutoService(TypeConverter::class)
class StringToUByteConverter : BaseTypeConverter(String::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class StringToCharConverter : BaseTypeConverter(String::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.get(0)"
}

@AutoService(TypeConverter::class)
class StringToBooleanConverter : BaseTypeConverter(String::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBoolean()"
}

@AutoService(TypeConverter::class)
class StringToFloatConverter : BaseTypeConverter(String::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class StringToDoubleConverter : BaseTypeConverter(String::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class StringToBigIntegerConverter : BaseTypeConverter(String::class, BigInteger::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.math.BigInteger(it) }"
}

@AutoService(TypeConverter::class)
class StringToBigDecimalConverter : BaseTypeConverter(String::class, BigDecimal::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let·{ java.math.BigDecimal(it) }"
}

// =============
// ==== INT ====
// =============

@AutoService(TypeConverter::class)
class IntToStringConverter : BaseTypeConverter(Int::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class IntToUIntConverter : BaseTypeConverter(Int::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class IntToLongConverter : BaseTypeConverter(Int::class, Long::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class IntToULongConverter : BaseTypeConverter(Int::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class IntToShortConverter : BaseTypeConverter(Int::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

@AutoService(TypeConverter::class)
class IntToUShortConverter : BaseTypeConverter(Int::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class IntToNumberConverter : BaseTypeConverter(Int::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

@AutoService(TypeConverter::class)
class IntToByteConverter : BaseTypeConverter(Int::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

@AutoService(TypeConverter::class)
class IntToUByteConverter : BaseTypeConverter(Int::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class IntToCharConverter : BaseTypeConverter(Int::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toChar()"
}

@AutoService(TypeConverter::class)
class IntToBooleanConverter : BaseTypeConverter(Int::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class IntToFloatConverter : BaseTypeConverter(Int::class, Float::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class IntToDoubleConverter : BaseTypeConverter(Int::class, Double::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class IntToBigIntegerConverter : BaseTypeConverter(Int::class, BigInteger::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class IntToBigDecimalConverter : BaseTypeConverter(Int::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBigDecimal()"
}

// =============
// ==== UINT ====
// =============

@AutoService(TypeConverter::class)
class UIntToStringConverter : BaseTypeConverter(UInt::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class UIntToIntConverter : BaseTypeConverter(UInt::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class UIntToLongConverter : BaseTypeConverter(UInt::class, Long::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class UIntToULongConverter : BaseTypeConverter(UInt::class, ULong::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class UIntToShortConverter : BaseTypeConverter(UInt::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

@AutoService(TypeConverter::class)
class UIntToUShortConverter : BaseTypeConverter(UInt::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class UIntToNumberConverter : BaseTypeConverter(UInt::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class UIntToByteConverter : BaseTypeConverter(UInt::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

@AutoService(TypeConverter::class)
class UIntToUByteConverter : BaseTypeConverter(UInt::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class UIntToCharConverter : BaseTypeConverter(UInt::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class UIntToBooleanConverter : BaseTypeConverter(UInt::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1u"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class UIntToFloatConverter : BaseTypeConverter(UInt::class, Float::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class UIntToDoubleConverter : BaseTypeConverter(UInt::class, Double::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class UIntToBigIntegerConverter : BaseTypeConverter(UInt::class, BigInteger::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class UIntToBigDecimalConverter : BaseTypeConverter(UInt::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigDecimal()"
}


// ==============
// ==== LONG ====
// ==============

@AutoService(TypeConverter::class)
class LongToStringConverter : BaseTypeConverter(Long::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class LongToIntConverter : BaseTypeConverter(Long::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class LongToUIntConverter : BaseTypeConverter(Long::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class LongToULongConverter : BaseTypeConverter(Long::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class LongToShortConverter : BaseTypeConverter(Long::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

@AutoService(TypeConverter::class)
class LongToUShortConverter : BaseTypeConverter(Long::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class LongToNumberConverter : BaseTypeConverter(Long::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

@AutoService(TypeConverter::class)
class LongToByteConverter : BaseTypeConverter(Long::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

@AutoService(TypeConverter::class)
class LongToUByteConverter : BaseTypeConverter(Long::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class LongToCharConverter : BaseTypeConverter(Long::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class LongToBooleanConverter : BaseTypeConverter(Long::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1L"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class LongToFloatConverter : BaseTypeConverter(Long::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class LongToDoubleConverter : BaseTypeConverter(Long::class, Double::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class LongToBigIntegerConverter : BaseTypeConverter(Long::class, BigInteger::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class LongToBigDecimalConverter : BaseTypeConverter(Long::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBigDecimal()"
}


// ===============
// ==== ULONG ====
// ===============

@AutoService(TypeConverter::class)
class ULongToStringConverter : BaseTypeConverter(ULong::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class ULongToIntConverter : BaseTypeConverter(ULong::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class ULongToUIntConverter : BaseTypeConverter(ULong::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class ULongToLongConverter : BaseTypeConverter(ULong::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class ULongToShortConverter : BaseTypeConverter(ULong::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

@AutoService(TypeConverter::class)
class ULongToUShortConverter : BaseTypeConverter(ULong::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class ULongToNumberConverter : BaseTypeConverter(ULong::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class ULongToByteConverter : BaseTypeConverter(ULong::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

@AutoService(TypeConverter::class)
class ULongToUByteConverter : BaseTypeConverter(ULong::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class ULongToCharConverter : BaseTypeConverter(ULong::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class ULongToBooleanConverter : BaseTypeConverter(ULong::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1u.toULong()"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class ULongToFloatConverter : BaseTypeConverter(ULong::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class ULongToDoubleConverter : BaseTypeConverter(ULong::class, Double::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class ULongToBigIntegerConverter : BaseTypeConverter(ULong::class, BigInteger::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class ULongToBigDecimalConverter : BaseTypeConverter(ULong::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigDecimal()"
}

// ===============
// ==== SHORT ====
// ===============

@AutoService(TypeConverter::class)
class ShortToStringConverter : BaseTypeConverter(Short::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class ShortToIntConverter : BaseTypeConverter(Short::class, Int::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class ShortToUIntConverter : BaseTypeConverter(Short::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class ShortToLongConverter : BaseTypeConverter(Short::class, Long::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class ShortToULongConverter : BaseTypeConverter(Short::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class ShortToUShortConverter : BaseTypeConverter(Short::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class ShortToNumberConverter : BaseTypeConverter(Short::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

@AutoService(TypeConverter::class)
class ShortToByteConverter : BaseTypeConverter(Short::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

@AutoService(TypeConverter::class)
class ShortToUByteConverter : BaseTypeConverter(Short::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class ShortToCharConverter : BaseTypeConverter(Short::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class ShortToBooleanConverter : BaseTypeConverter(Short::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1.toShort()"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class ShortToFloatConverter : BaseTypeConverter(Short::class, Float::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class ShortToDoubleConverter : BaseTypeConverter(Short::class, Double::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class ShortToBigIntegerConverter : BaseTypeConverter(Short::class, BigInteger::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class ShortToBigDecimalConverter : BaseTypeConverter(Short::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigDecimal()"
}

// ================
// ==== USHORT ====
// ================

@AutoService(TypeConverter::class)
class UShortToStringConverter : BaseTypeConverter(UShort::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class UShortToIntConverter : BaseTypeConverter(UShort::class, Int::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class UShortToUIntConverter : BaseTypeConverter(UShort::class, UInt::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class UShortToLongConverter : BaseTypeConverter(UShort::class, Long::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class UShortToULongConverter : BaseTypeConverter(UShort::class, ULong::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class UShortToShortConverter : BaseTypeConverter(UShort::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

@AutoService(TypeConverter::class)
class UShortToNumberConverter : BaseTypeConverter(UShort::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class UShortToByteConverter : BaseTypeConverter(UShort::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

@AutoService(TypeConverter::class)
class UShortToUByteConverter : BaseTypeConverter(UShort::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class UShortToCharConverter : BaseTypeConverter(UShort::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class UShortToBooleanConverter : BaseTypeConverter(UShort::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1u.toUShort()"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class UShortToFloatConverter : BaseTypeConverter(UShort::class, Float::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class UShortToDoubleConverter : BaseTypeConverter(UShort::class, Double::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class UShortToBigIntegerConverter : BaseTypeConverter(UShort::class, BigInteger::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class UShortToBigDecimalConverter : BaseTypeConverter(UShort::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigDecimal()"
}

// ===============
// ==== FLOAT ====
// ===============

@AutoService(TypeConverter::class)
class FloatToStringConverter : BaseTypeConverter(Float::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class FloatToIntConverter : BaseTypeConverter(Float::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class FloatToUIntConverter : BaseTypeConverter(Float::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class FloatToLongConverter : BaseTypeConverter(Float::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class FloatToULongConverter : BaseTypeConverter(Float::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class FloatToShortConverter : BaseTypeConverter(Float::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toShort()"
}

@AutoService(TypeConverter::class)
class FloatToUShortConverter : BaseTypeConverter(Float::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class FloatToNumberConverter : BaseTypeConverter(Float::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

@AutoService(TypeConverter::class)
class FloatToByteConverter : BaseTypeConverter(Float::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toByte()"
}

@AutoService(TypeConverter::class)
class FloatToUByteConverter : BaseTypeConverter(Float::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class FloatToCharConverter : BaseTypeConverter(Float::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class FloatToBooleanConverter : BaseTypeConverter(Float::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1.0f"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class FloatToDoubleConverter : BaseTypeConverter(Float::class, Double::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class FloatToBigIntegerConverter : BaseTypeConverter(Float::class, BigInteger::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class FloatToBigDecimalConverter : BaseTypeConverter(Float::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBigDecimal()"
}

// ================
// ==== DOUBLE ====
// ================

@AutoService(TypeConverter::class)
class DoubleToStringConverter : BaseTypeConverter(Double::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class DoubleToIntConverter : BaseTypeConverter(Double::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class DoubleToUIntConverter : BaseTypeConverter(Double::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class DoubleToLongConverter : BaseTypeConverter(Double::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class DoubleToULongConverter : BaseTypeConverter(Double::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class DoubleToShortConverter : BaseTypeConverter(Double::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toShort()"
}

@AutoService(TypeConverter::class)
class DoubleToUShortConverter : BaseTypeConverter(Double::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class DoubleToNumberConverter : BaseTypeConverter(Double::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

@AutoService(TypeConverter::class)
class DoubleToByteConverter : BaseTypeConverter(Double::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toByte()"
}

@AutoService(TypeConverter::class)
class DoubleToUByteConverter : BaseTypeConverter(Double::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class DoubleToCharConverter : BaseTypeConverter(Double::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class DoubleToBooleanConverter : BaseTypeConverter(Double::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1.0"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class DoubleToFloatConverter : BaseTypeConverter(Double::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class DoubleToBigIntegerConverter : BaseTypeConverter(Double::class, BigInteger::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class DoubleToBigDecimalConverter : BaseTypeConverter(Double::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBigDecimal()"
}

// ==============
// ==== BYTE ====
// ==============

@AutoService(TypeConverter::class)
class ByteToStringConverter : BaseTypeConverter(Byte::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class ByteToIntConverter : BaseTypeConverter(Byte::class, Int::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class ByteToUIntConverter : BaseTypeConverter(Byte::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class ByteToLongConverter : BaseTypeConverter(Byte::class, Long::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class ByteToULongConverter : BaseTypeConverter(Byte::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class ByteToShortConverter : BaseTypeConverter(Byte::class, Short::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toShort()"
}

@AutoService(TypeConverter::class)
class ByteToUShortConverter : BaseTypeConverter(Byte::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class ByteToNumberConverter : BaseTypeConverter(Byte::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

@AutoService(TypeConverter::class)
class ByteToUByteConverter : BaseTypeConverter(Byte::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class ByteToDoubleConverter : BaseTypeConverter(Byte::class, Double::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class ByteToCharConverter : BaseTypeConverter(Byte::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class ByteToBooleanConverter : BaseTypeConverter(Byte::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1.toByte()"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class ByteToFloatConverter : BaseTypeConverter(Byte::class, Float::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class ByteToBigIntegerConverter : BaseTypeConverter(Byte::class, BigInteger::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class ByteToBigDecimalConverter : BaseTypeConverter(Byte::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigDecimal()"
}


// ================
// ==== UBYTE ====
// ================

@AutoService(TypeConverter::class)
class UByteToStringConverter : BaseTypeConverter(UByte::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class UByteToIntConverter : BaseTypeConverter(UByte::class, Int::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class UByteToUIntConverter : BaseTypeConverter(UByte::class, UInt::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class UByteToLongConverter : BaseTypeConverter(UByte::class, Long::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class UByteToULongConverter : BaseTypeConverter(UByte::class, ULong::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

@AutoService(TypeConverter::class)
class UByteToShortConverter : BaseTypeConverter(UByte::class, Short::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

@AutoService(TypeConverter::class)
class UByteToUShortConverter : BaseTypeConverter(UByte::class, UShort::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class UByteToNumberConverter : BaseTypeConverter(UByte::class, Number::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

@AutoService(TypeConverter::class)
class UByteToByteConverter : BaseTypeConverter(UByte::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

@AutoService(TypeConverter::class)
class UByteToCharConverter : BaseTypeConverter(UByte::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class UByteToBooleanConverter : BaseTypeConverter(UByte::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1u.toUByte()"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class UByteToFloatConverter : BaseTypeConverter(UByte::class, Float::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class UByteToDoubleConverter : BaseTypeConverter(UByte::class, Double::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class UByteToBigIntegerConverter : BaseTypeConverter(UByte::class, BigInteger::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class UByteToBigDecimalConverter : BaseTypeConverter(UByte::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigDecimal()"
}

// ================
// ==== NUMBER ====
// ================

@AutoService(TypeConverter::class)
class NumberToStringConverter : BaseTypeConverter(Number::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class NumberToIntConverter : BaseTypeConverter(Number::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

@AutoService(TypeConverter::class)
class NumberToUIntConverter : BaseTypeConverter(Number::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class NumberToLongConverter : BaseTypeConverter(Number::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

@AutoService(TypeConverter::class)
class NumberToULongConverter : BaseTypeConverter(Number::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toULong()"
}

@AutoService(TypeConverter::class)
class NumberToShortConverter : BaseTypeConverter(Number::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

@AutoService(TypeConverter::class)
class NumberToUShortConverter : BaseTypeConverter(Number::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class NumberToByteConverter : BaseTypeConverter(Number::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

@AutoService(TypeConverter::class)
class NumberToUByteConverter : BaseTypeConverter(Number::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class NumberToCharConverter : BaseTypeConverter(Number::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toChar()"
}

@AutoService(TypeConverter::class)
class NumberToBooleanConverter : BaseTypeConverter(Number::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·1"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class NumberToFloatConverter : BaseTypeConverter(Number::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class NumberToDoubleConverter : BaseTypeConverter(Number::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class NumberToBigIntegerConverter : BaseTypeConverter(Number::class, BigInteger::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class NumberToBigDecimalConverter : BaseTypeConverter(Number::class, BigDecimal::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()$nc.toBigDecimal()"
}


// ==============
// ==== CHAR ====
// ==============

@AutoService(TypeConverter::class)
class CharToStringConverter : BaseTypeConverter(Char::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class CharToIntConverter : BaseTypeConverter(Char::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code"
}

@AutoService(TypeConverter::class)
class CharToUIntConverter : BaseTypeConverter(Char::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class CharToLongConverter : BaseTypeConverter(Char::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toLong()"
}

@AutoService(TypeConverter::class)
class CharToULongConverter : BaseTypeConverter(Char::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toULong()"
}

@AutoService(TypeConverter::class)
class CharToShortConverter : BaseTypeConverter(Char::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toShort()"
}

@AutoService(TypeConverter::class)
class CharToUShortConverter : BaseTypeConverter(Char::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class CharToNumberConverter : BaseTypeConverter(Char::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code"
}

@AutoService(TypeConverter::class)
class CharToDoubleConverter : BaseTypeConverter(Char::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class CharToByteConverter : BaseTypeConverter(Char::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toByte()"
}

@AutoService(TypeConverter::class)
class CharToUByteConverter : BaseTypeConverter(Char::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class CharToBooleanConverter : BaseTypeConverter(Char::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·'1'"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class CharToFloatConverter : BaseTypeConverter(Char::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class CharToBigIntegerConverter : BaseTypeConverter(Char::class, BigInteger::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toLong()$nc.toBigInteger()"
}

@AutoService(TypeConverter::class)
class CharToBigDecimalConverter : BaseTypeConverter(Char::class, BigDecimal::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toLong()$nc.toBigDecimal()"
}

// =================
// ==== BOOLEAN ====
// =================

@AutoService(TypeConverter::class)
class BooleanToStringConverter : BaseTypeConverter(Boolean::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class BooleanToIntConverter : BaseTypeConverter(Boolean::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1·else·0"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToUIntConverter : BaseTypeConverter(Boolean::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1u·else·0u"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToLongConverter : BaseTypeConverter(Boolean::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1L·else·0L"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToULongConverter : BaseTypeConverter(Boolean::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1u·else·0u"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToShortConverter : BaseTypeConverter(Boolean::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1.toShort()·else·0.toShort()"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToUShortConverter : BaseTypeConverter(Boolean::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1u·else·0u"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToNumberConverter : BaseTypeConverter(Boolean::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1·else·0"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToDoubleConverter : BaseTypeConverter(Boolean::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1.0·else·0.0"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToByteConverter : BaseTypeConverter(Boolean::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1·else·0"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToUByteConverter : BaseTypeConverter(Boolean::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1u·else·0u"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToCharConverter : BaseTypeConverter(Boolean::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·'1'·else·'0'"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToFloatConverter : BaseTypeConverter(Boolean::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "if·($fieldName·==·true)·1.0f·else·0.0f"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToBigIntegerConverter : BaseTypeConverter(Boolean::class, BigInteger::class) {
    override fun convert(fieldName: String, nc: String): String =
        "if·($fieldName·==·true)·java.math.BigInteger.ONE·else·java.math.BigInteger.ZERO"

    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BooleanToBigDecimalConverter : BaseTypeConverter(Boolean::class, BigDecimal::class) {
    override fun convert(fieldName: String, nc: String): String =
        "if·($fieldName·==·true)·java.math.BigDecimal.ONE·else·java.math.BigDecimal.ZERO"

    override fun appendNotNullAssertionOperator(code: String): String = code
}

// ================
// == BIGINTEGER ==
// ================

@AutoService(TypeConverter::class)
class BigIntegerToStringConverter : BaseTypeConverter(BigInteger::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class BigIntegerToIntConverter : BaseTypeConverter(BigInteger::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.intValueExact()"
}

@AutoService(TypeConverter::class)
class BigIntegerToUIntConverter : BaseTypeConverter(BigInteger::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.intValueExact()$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class BigIntegerToLongConverter : BaseTypeConverter(BigInteger::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.longValueExact()"
}

@AutoService(TypeConverter::class)
class BigIntegerToULongConverter : BaseTypeConverter(BigInteger::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.longValueExact()$nc.toULong()"
}

@AutoService(TypeConverter::class)
class BigIntegerToShortConverter : BaseTypeConverter(BigInteger::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.shortValueExact()"
}

@AutoService(TypeConverter::class)
class BigIntegerToUShortConverter : BaseTypeConverter(BigInteger::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.shortValueExact()$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class BigIntegerToByteConverter : BaseTypeConverter(BigInteger::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.byteValueExact()"
}

@AutoService(TypeConverter::class)
class BigIntegerToUByteConverter : BaseTypeConverter(BigInteger::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.byteValueExact()$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class BigIntegerToCharConverter : BaseTypeConverter(BigInteger::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.intValueExact()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class BigIntegerToBooleanConverter : BaseTypeConverter(BigInteger::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·java.math.BigInteger.ONE"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BigIntegerToFloatConverter : BaseTypeConverter(BigInteger::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class BigIntegerToDoubleConverter : BaseTypeConverter(BigInteger::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class BigIntegerToNumberConverter : BaseTypeConverter(BigInteger::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

@AutoService(TypeConverter::class)
class BigIntegerToBigDecimalConverter : BaseTypeConverter(BigInteger::class, BigDecimal::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBigDecimal()"
}

// ================
// == BIGDECIMAL ==
// ================

@AutoService(TypeConverter::class)
class BigDecimalToStringConverter : BaseTypeConverter(BigDecimal::class, String::class, true) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

@AutoService(TypeConverter::class)
class BigDecimalToIntConverter : BaseTypeConverter(BigDecimal::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.intValueExact()"
}

@AutoService(TypeConverter::class)
class BigDecimalToUIntConverter : BaseTypeConverter(BigDecimal::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.intValueExact()$nc.toUInt()"
}

@AutoService(TypeConverter::class)
class BigDecimalToLongConverter : BaseTypeConverter(BigDecimal::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.longValueExact()"
}

@AutoService(TypeConverter::class)
class BigDecimalToULongConverter : BaseTypeConverter(BigDecimal::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.longValueExact()$nc.toULong()"
}

@AutoService(TypeConverter::class)
class BigDecimalToShortConverter : BaseTypeConverter(BigDecimal::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.shortValueExact()"
}

@AutoService(TypeConverter::class)
class BigDecimalToUShortConverter : BaseTypeConverter(BigDecimal::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.shortValueExact()$nc.toUShort()"
}

@AutoService(TypeConverter::class)
class BigDecimalToByteConverter : BaseTypeConverter(BigDecimal::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.byteValueExact()"
}

@AutoService(TypeConverter::class)
class BigDecimalToUByteConverter : BaseTypeConverter(BigDecimal::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.byteValueExact()$nc.toUByte()"
}

@AutoService(TypeConverter::class)
class BigDecimalToCharConverter : BaseTypeConverter(BigDecimal::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.intValueExact()$nc.toChar()"
}

@AutoService(TypeConverter::class)
class BigDecimalToBooleanConverter : BaseTypeConverter(BigDecimal::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName·==·java.math.BigDecimal.ONE"
    override fun appendNotNullAssertionOperator(code: String): String = code
}

@AutoService(TypeConverter::class)
class BigDecimalToFloatConverter : BaseTypeConverter(BigDecimal::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

@AutoService(TypeConverter::class)
class BigDecimalToDoubleConverter : BaseTypeConverter(BigDecimal::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

@AutoService(TypeConverter::class)
class BigDecimalToNumberConverter : BaseTypeConverter(BigDecimal::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

@AutoService(TypeConverter::class)
class BigDecimalToBigIntegerConverter : BaseTypeConverter(BigDecimal::class, BigInteger::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBigInteger()"
}
