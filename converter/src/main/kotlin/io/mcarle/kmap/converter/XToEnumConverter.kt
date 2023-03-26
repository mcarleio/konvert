package io.mcarle.kmap.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.kmap.converter.api.TypeConverter
import io.mcarle.kmap.converter.api.isNullable
import kotlin.reflect.KClass

abstract class XToEnumConverter(
    internal val sourceClass: KClass<*>
) : AbstractTypeConverter() {

    private val enumType: KSType by lazy {
        resolver.getClassDeclarationByName<Enum<*>>()!!.asStarProjectedType()
    }

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asStarProjectedType()
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            enumType != targetNotNullable && enumType.isAssignableFrom(targetNotNullable)
                && sourceType == sourceNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "", target.declaration.qualifiedName!!.asString())

        return convertCode + appendNotNullAssertionOperatorIfNeeded(source, target)
    }

    override val enabledByDefault: Boolean = false

    abstract fun convert(fieldName: String, nc: String, enumFQ: String): String

}

@AutoService(TypeConverter::class)
class StringToEnumConverter : XToEnumConverter(String::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.valueOf(it) }"
    }
}

@AutoService(TypeConverter::class)
class IntToEnumConverter : XToEnumConverter(Int::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it] }"
    }
}

@AutoService(TypeConverter::class)
class UIntToEnumConverter : XToEnumConverter(UInt::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}

@AutoService(TypeConverter::class)
class LongToEnumConverter : XToEnumConverter(Long::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}

@AutoService(TypeConverter::class)
class ULongToEnumConverter : XToEnumConverter(ULong::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}

@AutoService(TypeConverter::class)
class ShortToEnumConverter : XToEnumConverter(Short::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}

@AutoService(TypeConverter::class)
class UShortToEnumConverter : XToEnumConverter(UShort::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}

@AutoService(TypeConverter::class)
class NumberToEnumConverter : XToEnumConverter(Number::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}

@AutoService(TypeConverter::class)
class DoubleToEnumConverter : XToEnumConverter(Double::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}

@AutoService(TypeConverter::class)
class ByteToEnumConverter : XToEnumConverter(Byte::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}

@AutoService(TypeConverter::class)
class UByteToEnumConverter : XToEnumConverter(UByte::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}

@AutoService(TypeConverter::class)
class FloatToEnumConverter : XToEnumConverter(Float::class) {
    override fun convert(fieldName: String, nc: String, enumFQ: String): String {
        return "$fieldName$nc.let·{ $enumFQ.values()[it.toInt()] }"
    }
}
