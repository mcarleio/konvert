package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.AbstractTypeConverter
import io.mcarle.lib.kmapper.processor.isNullable
import kotlin.reflect.KClass

abstract class EnumToXConverter(
    internal val targetClass: KClass<*>
) : AbstractTypeConverter() {

    private val enumType: KSType by lazy {
        resolver.getClassDeclarationByName<Enum<*>>()!!.asStarProjectedType()
    }

    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asStarProjectedType()
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            enumType.isAssignableFrom(sourceNotNullable) && targetType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "")

        return convertCode + appendNotNullAssertionOperatorIfNeeded(source, target)
    }

    abstract fun convert(fieldName: String, nc: String): String

}

class EnumToStringConverter : EnumToXConverter(String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.name"
}

class EnumToIntConverter : EnumToXConverter(Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal"
}

class EnumToUIntConverter : EnumToXConverter(UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toUInt()"
}

class EnumToLongConverter : EnumToXConverter(Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toLong()"
}

class EnumToULongConverter : EnumToXConverter(ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toULong()"
}

class EnumToShortConverter : EnumToXConverter(Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toShort()"
}

class EnumToUShortConverter : EnumToXConverter(UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toUShort()"
}

class EnumToNumberConverter : EnumToXConverter(Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal"
}

class EnumToDoubleConverter : EnumToXConverter(Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toDouble()"
}

class EnumToByteConverter : EnumToXConverter(Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toByte()"
}

class EnumToUByteConverter : EnumToXConverter(UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toUByte()"
}

class EnumToCharConverter : EnumToXConverter(Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toChar()"
}

class EnumToFloatConverter : EnumToXConverter(Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toFloat()"
}