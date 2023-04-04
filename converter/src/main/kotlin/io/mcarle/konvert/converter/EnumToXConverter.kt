package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.isNullable
import kotlin.reflect.KClass

abstract class EnumToXConverter(
    val targetClass: KClass<*>
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

@AutoService(TypeConverter::class)
class EnumToStringConverter : EnumToXConverter(String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.name"
    override val enabledByDefault: Boolean = true
}

@AutoService(TypeConverter::class)
class EnumToIntConverter : EnumToXConverter(Int::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal"
    override val enabledByDefault: Boolean = true
}

@AutoService(TypeConverter::class)
class EnumToUIntConverter : EnumToXConverter(UInt::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toUInt()"
    override val enabledByDefault: Boolean = true
}

@AutoService(TypeConverter::class)
class EnumToLongConverter : EnumToXConverter(Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toLong()"
    override val enabledByDefault: Boolean = true
}

@AutoService(TypeConverter::class)
class EnumToULongConverter : EnumToXConverter(ULong::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toULong()"
    override val enabledByDefault: Boolean = true
}

@AutoService(TypeConverter::class)
class EnumToShortConverter : EnumToXConverter(Short::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toShort()"
    override val enabledByDefault: Boolean = true
}

@AutoService(TypeConverter::class)
class EnumToUShortConverter : EnumToXConverter(UShort::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toUShort()"
    override val enabledByDefault: Boolean = true
}

@AutoService(TypeConverter::class)
class EnumToNumberConverter : EnumToXConverter(Number::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal"
    override val enabledByDefault: Boolean = true
}

@AutoService(TypeConverter::class)
class EnumToDoubleConverter : EnumToXConverter(Double::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toDouble()"
    override val enabledByDefault: Boolean = false
}

@AutoService(TypeConverter::class)
class EnumToByteConverter : EnumToXConverter(Byte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toByte()"
    override val enabledByDefault: Boolean = false
}

@AutoService(TypeConverter::class)
class EnumToUByteConverter : EnumToXConverter(UByte::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toUByte()"
    override val enabledByDefault: Boolean = false
}

@AutoService(TypeConverter::class)
class EnumToCharConverter : EnumToXConverter(Char::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toChar()"
    override val enabledByDefault: Boolean = false
}

@AutoService(TypeConverter::class)
class EnumToFloatConverter : EnumToXConverter(Float::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.ordinal$nc.toFloat()"
    override val enabledByDefault: Boolean = false
}
