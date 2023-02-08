package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.isNullable
import kotlin.reflect.KClass

abstract class XToEnumConverter(
    internal val sourceClass: KClass<*>
) : AbstractTypeConverter() {

    private val enumType: KSType by lazy {
        resolver.getClassDeclarationByName<Enum<*>>()!!.asStarProjectedType().makeNullable()
    }

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asType(emptyList())
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return enumType != target && enumType.isAssignableFrom(target) && (sourceType == source || sourceType == source.makeNotNullable())
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "", target.declaration.qualifiedName!!.asString())

        return if (sourceNullable && !target.isNullable()) {
            "$convertCode!!"
        } else {
            convertCode
        }
    }

    abstract fun convert(fieldName: String, nc: String, enumFQ: String): String

}


class StringToEnumConverter : XToEnumConverter(String::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.valueOf(it) }"
    }
}

class IntToEnumConverter : XToEnumConverter(Int::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it] }"
    }
}

class UIntToEnumConverter : XToEnumConverter(UInt::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}

class LongToEnumConverter : XToEnumConverter(Long::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}

class ULongToEnumConverter : XToEnumConverter(ULong::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}

class ShortToEnumConverter : XToEnumConverter(Short::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}

class UShortToEnumConverter : XToEnumConverter(UShort::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}

class NumberToEnumConverter : XToEnumConverter(Number::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}

class DoubleToEnumConverter : XToEnumConverter(Double::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}

class ByteToEnumConverter : XToEnumConverter(Byte::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}

class UByteToEnumConverter : XToEnumConverter(UByte::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}

class FloatToEnumConverter : XToEnumConverter(Float::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let { $enumFQ.values()[it.toInt()] }"
    }
}