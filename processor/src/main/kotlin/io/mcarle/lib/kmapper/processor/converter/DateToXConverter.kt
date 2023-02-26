package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.AbstractTypeConverter
import io.mcarle.lib.kmapper.processor.isNullable
import java.time.Instant
import kotlin.reflect.KClass

abstract class DateToXConverter(
    internal val targetClass: KClass<*>
) : AbstractTypeConverter() {

    private val dateType: KSType by lazy {
        resolver.getClassDeclarationByName("java.util.Date")!!.asStarProjectedType()
    }

    private val targetType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asStarProjectedType()
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            dateType.isAssignableFrom(sourceNotNullable) && targetType == targetNotNullable
        }
//        return dateType.isAssignableFrom(source) && (targetType == target || targetType == target.makeNotNullable())
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "")

        return convertCode + appendNotNullAssertionOperatorIfNeeded(source, target)
    }

    abstract fun convert(fieldName: String, nc: String): String
}

class DateToStringConverter : DateToXConverter(String::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()$nc.toString()"
}

class DateToLongConverter : DateToXConverter(Long::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.time"
}

class DateToInstantConverter : DateToXConverter(Instant::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.toInstant()"
}