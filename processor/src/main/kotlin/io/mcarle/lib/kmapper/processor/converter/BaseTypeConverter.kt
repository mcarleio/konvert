package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.isNullable
import kotlin.reflect.KClass

abstract class BaseTypeConverter(
    internal val sourceClass: KClass<*>,
    internal val targetClass: KClass<*>
) : AbstractTypeConverter() {

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asType(emptyList())
    }
    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asType(emptyList())
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        if (sourceType == source && targetType == target) {
            return true
        }

        val nonNullSource = source.makeNotNullable()
        val nonNullTarget = target.makeNotNullable()

        return sourceType == nonNullSource && targetType == nonNullTarget
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "")

        return if (sourceNullable && !target.isNullable()) {
            handleNullable(convertCode)
        } else {
            convertCode
        }
    }

    abstract fun convert(fieldName: String, nc: String): String
    protected open fun handleNullable(code: String): String = "$code!!"

}

// ================
// ==== STRING ====
// ================

class StringToIntConverter : BaseTypeConverter(String::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class StringToUIntConverter : BaseTypeConverter(String::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class StringToLongConverter : BaseTypeConverter(String::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class StringToULongConverter : BaseTypeConverter(String::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class StringToShortConverter : BaseTypeConverter(String::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

class StringToUShortConverter : BaseTypeConverter(String::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

class StringToNumberConverter : BaseTypeConverter(String::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

class StringToByteConverter : BaseTypeConverter(String::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class StringToUByteConverter : BaseTypeConverter(String::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

class StringToCharConverter : BaseTypeConverter(String::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.get(0)"
}

class StringToBooleanConverter : BaseTypeConverter(String::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toBoolean()"
}

class StringToFloatConverter : BaseTypeConverter(String::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

class StringToDoubleConverter : BaseTypeConverter(String::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}


// =============
// ==== INT ====
// =============

class IntToStringConverter : BaseTypeConverter(Int::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class IntToUIntConverter : BaseTypeConverter(Int::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class IntToLongConverter : BaseTypeConverter(Int::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class IntToULongConverter : BaseTypeConverter(Int::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class IntToShortConverter : BaseTypeConverter(Int::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

class IntToUShortConverter : BaseTypeConverter(Int::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

class IntToNumberConverter : BaseTypeConverter(Int::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

class IntToByteConverter : BaseTypeConverter(Int::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class IntToUByteConverter : BaseTypeConverter(Int::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

class IntToCharConverter : BaseTypeConverter(Int::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toChar()"
}

class IntToBooleanConverter : BaseTypeConverter(Int::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1"
    override fun handleNullable(code: String): String = code
}

class IntToFloatConverter : BaseTypeConverter(Int::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

class IntToDoubleConverter : BaseTypeConverter(Int::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}


// =============
// ==== UINT ====
// =============

class UIntToStringConverter : BaseTypeConverter(UInt::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class UIntToIntConverter : BaseTypeConverter(UInt::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class UIntToLongConverter : BaseTypeConverter(UInt::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class UIntToULongConverter : BaseTypeConverter(UInt::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class UIntToShortConverter : BaseTypeConverter(UInt::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

class UIntToUShortConverter : BaseTypeConverter(UInt::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

class UIntToNumberConverter : BaseTypeConverter(UInt::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class UIntToByteConverter : BaseTypeConverter(UInt::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class UIntToUByteConverter : BaseTypeConverter(UInt::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

class UIntToCharConverter : BaseTypeConverter(UInt::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

class UIntToBooleanConverter : BaseTypeConverter(UInt::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1u"
    override fun handleNullable(code: String): String = code
}

class UIntToFloatConverter : BaseTypeConverter(UInt::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

class UIntToDoubleConverter : BaseTypeConverter(UInt::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}


// ==============
// ==== LONG ====
// ==============

class LongToStringConverter : BaseTypeConverter(Long::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class LongToIntConverter : BaseTypeConverter(Long::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class LongToUIntConverter : BaseTypeConverter(Long::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class LongToULongConverter : BaseTypeConverter(Long::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class LongToShortConverter : BaseTypeConverter(Long::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

class LongToUShortConverter : BaseTypeConverter(Long::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

class LongToNumberConverter : BaseTypeConverter(Long::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

class LongToByteConverter : BaseTypeConverter(Long::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class LongToUByteConverter : BaseTypeConverter(Long::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

class LongToCharConverter : BaseTypeConverter(Long::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

class LongToBooleanConverter : BaseTypeConverter(Long::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1L"
    override fun handleNullable(code: String): String = code
}

class LongToFloatConverter : BaseTypeConverter(Long::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

class LongToDoubleConverter : BaseTypeConverter(Long::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}


// ===============
// ==== ULONG ====
// ===============

class ULongToStringConverter : BaseTypeConverter(ULong::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class ULongToIntConverter : BaseTypeConverter(ULong::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class ULongToUIntConverter : BaseTypeConverter(ULong::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class ULongToLongConverter : BaseTypeConverter(ULong::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class ULongToShortConverter : BaseTypeConverter(ULong::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

class ULongToUShortConverter : BaseTypeConverter(ULong::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

class ULongToNumberConverter : BaseTypeConverter(ULong::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class ULongToByteConverter : BaseTypeConverter(ULong::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class ULongToUByteConverter : BaseTypeConverter(ULong::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

class ULongToCharConverter : BaseTypeConverter(ULong::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

class ULongToBooleanConverter : BaseTypeConverter(ULong::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1u.toULong()"
    override fun handleNullable(code: String): String = code
}

class ULongToFloatConverter : BaseTypeConverter(ULong::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

class ULongToDoubleConverter : BaseTypeConverter(ULong::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

// ===============
// ==== SHORT ====
// ===============

class ShortToStringConverter : BaseTypeConverter(Short::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class ShortToIntConverter : BaseTypeConverter(Short::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class ShortToUIntConverter : BaseTypeConverter(Short::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class ShortToLongConverter : BaseTypeConverter(Short::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class ShortToULongConverter : BaseTypeConverter(Short::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class ShortToUShortConverter : BaseTypeConverter(Short::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

class ShortToNumberConverter : BaseTypeConverter(Short::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

class ShortToByteConverter : BaseTypeConverter(Short::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class ShortToUByteConverter : BaseTypeConverter(Short::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

class ShortToCharConverter : BaseTypeConverter(Short::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

class ShortToBooleanConverter : BaseTypeConverter(Short::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1.toShort()"
    override fun handleNullable(code: String): String = code
}

class ShortToFloatConverter : BaseTypeConverter(Short::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

class ShortToDoubleConverter : BaseTypeConverter(Short::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}


// ================
// ==== USHORT ====
// ================

class UShortToStringConverter : BaseTypeConverter(UShort::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class UShortToIntConverter : BaseTypeConverter(UShort::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class UShortToUIntConverter : BaseTypeConverter(UShort::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class UShortToLongConverter : BaseTypeConverter(UShort::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class UShortToULongConverter : BaseTypeConverter(UShort::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class UShortToShortConverter : BaseTypeConverter(UShort::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

class UShortToNumberConverter : BaseTypeConverter(UShort::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

class UShortToByteConverter : BaseTypeConverter(UShort::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class UShortToUByteConverter : BaseTypeConverter(UShort::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

class UShortToCharConverter : BaseTypeConverter(UShort::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

class UShortToBooleanConverter : BaseTypeConverter(UShort::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1u.toUShort()"
    override fun handleNullable(code: String): String = code
}

class UShortToFloatConverter : BaseTypeConverter(UShort::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

class UShortToDoubleConverter : BaseTypeConverter(UShort::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}


// ===============
// ==== FLOAT ====
// ===============

class FloatToStringConverter : BaseTypeConverter(Float::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class FloatToIntConverter : BaseTypeConverter(Float::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class FloatToUIntConverter : BaseTypeConverter(Float::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class FloatToLongConverter : BaseTypeConverter(Float::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class FloatToULongConverter : BaseTypeConverter(Float::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class FloatToShortConverter : BaseTypeConverter(Float::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toShort()"
}

class FloatToUShortConverter : BaseTypeConverter(Float::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUShort()"
}

class FloatToNumberConverter : BaseTypeConverter(Float::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

class FloatToByteConverter : BaseTypeConverter(Float::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toByte()"
}

class FloatToUByteConverter : BaseTypeConverter(Float::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUByte()"
}

class FloatToCharConverter : BaseTypeConverter(Float::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

class FloatToBooleanConverter : BaseTypeConverter(Float::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1.0f"
    override fun handleNullable(code: String): String = code
}

class FloatToDoubleConverter : BaseTypeConverter(Float::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

// ================
// ==== DOUBLE ====
// ================

class DoubleToStringConverter : BaseTypeConverter(Double::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class DoubleToIntConverter : BaseTypeConverter(Double::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class DoubleToUIntConverter : BaseTypeConverter(Double::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class DoubleToLongConverter : BaseTypeConverter(Double::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class DoubleToULongConverter : BaseTypeConverter(Double::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class DoubleToShortConverter : BaseTypeConverter(Double::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toShort()"
}

class DoubleToUShortConverter : BaseTypeConverter(Double::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUShort()"
}

class DoubleToNumberConverter : BaseTypeConverter(Double::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

class DoubleToByteConverter : BaseTypeConverter(Double::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toByte()"
}

class DoubleToUByteConverter : BaseTypeConverter(Double::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUByte()"
}

class DoubleToCharConverter : BaseTypeConverter(Double::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

class DoubleToBooleanConverter : BaseTypeConverter(Double::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1.0"
    override fun handleNullable(code: String): String = code
}

class DoubleToFloatConverter : BaseTypeConverter(Double::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

// ==============
// ==== BYTE ====
// ==============

class ByteToStringConverter : BaseTypeConverter(Byte::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class ByteToIntConverter : BaseTypeConverter(Byte::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class ByteToUIntConverter : BaseTypeConverter(Byte::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class ByteToLongConverter : BaseTypeConverter(Byte::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class ByteToULongConverter : BaseTypeConverter(Byte::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class ByteToShortConverter : BaseTypeConverter(Byte::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toShort()"
}

class ByteToUShortConverter : BaseTypeConverter(Byte::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUShort()"
}

class ByteToNumberConverter : BaseTypeConverter(Byte::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = fieldName
}

class ByteToUByteConverter : BaseTypeConverter(Byte::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUByte()"
}

class ByteToDoubleConverter : BaseTypeConverter(Byte::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

class ByteToCharConverter : BaseTypeConverter(Byte::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

class ByteToBooleanConverter : BaseTypeConverter(Byte::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1.toByte()"
    override fun handleNullable(code: String): String = code
}

class ByteToFloatConverter : BaseTypeConverter(Byte::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}


// ================
// ==== UBYTE ====
// ================

class UByteToStringConverter : BaseTypeConverter(UByte::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class UByteToIntConverter : BaseTypeConverter(UByte::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class UByteToUIntConverter : BaseTypeConverter(UByte::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUInt()"
}

class UByteToLongConverter : BaseTypeConverter(UByte::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class UByteToULongConverter : BaseTypeConverter(UByte::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toULong()"
}

class UByteToShortConverter : BaseTypeConverter(UByte::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

class UByteToUShortConverter : BaseTypeConverter(UByte::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toUShort()"
}

class UByteToNumberConverter : BaseTypeConverter(UByte::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class UByteToByteConverter : BaseTypeConverter(UByte::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class UByteToCharConverter : BaseTypeConverter(UByte::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toChar()"
}

class UByteToBooleanConverter : BaseTypeConverter(UByte::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1u.toUByte()"
    override fun handleNullable(code: String): String = code
}

class UByteToFloatConverter : BaseTypeConverter(UByte::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

class UByteToDoubleConverter : BaseTypeConverter(UByte::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}

// ================
// ==== NUMBER ====
// ================

class NumberToStringConverter : BaseTypeConverter(Number::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class NumberToIntConverter : BaseTypeConverter(Number::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()"
}

class NumberToUIntConverter : BaseTypeConverter(Number::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInt()$nc.toUInt()"
}

class NumberToLongConverter : BaseTypeConverter(Number::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()"
}

class NumberToULongConverter : BaseTypeConverter(Number::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toLong()$nc.toULong()"
}

class NumberToShortConverter : BaseTypeConverter(Number::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()"
}

class NumberToUShortConverter : BaseTypeConverter(Number::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toShort()$nc.toUShort()"
}

class NumberToByteConverter : BaseTypeConverter(Number::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()"
}

class NumberToUByteConverter : BaseTypeConverter(Number::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toByte()$nc.toUByte()"
}

class NumberToCharConverter : BaseTypeConverter(Number::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toChar()"
}

class NumberToBooleanConverter : BaseTypeConverter(Number::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == 1"
    override fun handleNullable(code: String): String = code
}

class NumberToFloatConverter : BaseTypeConverter(Number::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toFloat()"
}

class NumberToDoubleConverter : BaseTypeConverter(Number::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toDouble()"
}


// ==============
// ==== CHAR ====
// ==============

class CharToStringConverter : BaseTypeConverter(Char::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class CharToIntConverter : BaseTypeConverter(Char::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code"
}

class CharToUIntConverter : BaseTypeConverter(Char::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toUInt()"
}

class CharToLongConverter : BaseTypeConverter(Char::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toLong()"
}

class CharToULongConverter : BaseTypeConverter(Char::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toULong()"
}

class CharToShortConverter : BaseTypeConverter(Char::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toShort()"
}

class CharToUShortConverter : BaseTypeConverter(Char::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toUShort()"
}

class CharToNumberConverter : BaseTypeConverter(Char::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code"
}

class CharToDoubleConverter : BaseTypeConverter(Char::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toDouble()"
}

class CharToByteConverter : BaseTypeConverter(Char::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toByte()"
}

class CharToUByteConverter : BaseTypeConverter(Char::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toUByte()"
}

class CharToBooleanConverter : BaseTypeConverter(Char::class, Boolean::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName == '1'"
    override fun handleNullable(code: String): String = code
}

class CharToFloatConverter : BaseTypeConverter(Char::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.code$nc.toFloat()"
}

// =================
// ==== BOOLEAN ====
// =================

class BooleanToStringConverter : BaseTypeConverter(Boolean::class, String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toString()"
}

class BooleanToIntConverter : BaseTypeConverter(Boolean::class, Int::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1 else 0"
    override fun handleNullable(code: String): String = code
}

class BooleanToUIntConverter : BaseTypeConverter(Boolean::class, UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1u else 0u"
    override fun handleNullable(code: String): String = code
}

class BooleanToLongConverter : BaseTypeConverter(Boolean::class, Long::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1L else 0L"
    override fun handleNullable(code: String): String = code
}

class BooleanToULongConverter : BaseTypeConverter(Boolean::class, ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1u else 0u"
    override fun handleNullable(code: String): String = code
}

class BooleanToShortConverter : BaseTypeConverter(Boolean::class, Short::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1.toShort() else 0.toShort()"
    override fun handleNullable(code: String): String = code
}

class BooleanToUShortConverter : BaseTypeConverter(Boolean::class, UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1u else 0u"
    override fun handleNullable(code: String): String = code
}

class BooleanToNumberConverter : BaseTypeConverter(Boolean::class, Number::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1 else 0"
    override fun handleNullable(code: String): String = code
}

class BooleanToDoubleConverter : BaseTypeConverter(Boolean::class, Double::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1.0 else 0.0"
    override fun handleNullable(code: String): String = code
}

class BooleanToByteConverter : BaseTypeConverter(Boolean::class, Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1 else 0"
    override fun handleNullable(code: String): String = code
}

class BooleanToUByteConverter : BaseTypeConverter(Boolean::class, UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1u else 0u"
    override fun handleNullable(code: String): String = code
}

class BooleanToCharConverter : BaseTypeConverter(Boolean::class, Char::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) '1' else '0'"
    override fun handleNullable(code: String): String = code
}

class BooleanToFloatConverter : BaseTypeConverter(Boolean::class, Float::class) {
    override fun convert(fieldName: String, nc: String): String = "if ($fieldName == true) 1.0f else 0.0f"
    override fun handleNullable(code: String): String = code
}