package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.isNullable
import kotlin.reflect.KClass

abstract class XToDateConverter(
    internal val sourceClass: KClass<*>
) : AbstractTypeConverter() {

    private val dateType: KSType by lazy {
        resolver.getClassDeclarationByName("java.util.Date")!!.asType(emptyList()).makeNullable()
    }

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asType(emptyList())
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return (dateType == target || dateType == target.makeNullable())
                && (sourceType == source || sourceType == source.makeNotNullable())
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "")

        return if (sourceNullable && !target.isNullable()) {
            "$convertCode!!"
        } else {
            convertCode
        }
    }

    abstract fun convert(fieldName: String, nc: String): String
}

class StringToDateConverter : XToDateConverter(String::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let { java.util.Date.from(java.time.Instant.parse(it)) }"
}

class LongToDateConverter : XToDateConverter(Long::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let { java.util.Date(it) }"
}