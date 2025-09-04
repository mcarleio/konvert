package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.isNullable

@AutoService(TypeConverter::class)
class ValueClassToXConverter : AbstractTypeConverter() {

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, _ ->
            val sourceClassDeclaration = sourceNotNullable.classDeclaration() ?: return@handleNullable false
            if (sourceClassDeclaration.classKind != ClassKind.CLASS) return@handleNullable false
            if (Modifier.VALUE !in sourceClassDeclaration.modifiers) return@handleNullable false

            val propertyType = sourceClassDeclaration.primaryConstructor
                ?.parameters
                ?.singleOrNull()
                ?.type
                ?.resolve()
                ?: return@handleNullable false

            return@handleNullable TypeConverterRegistry.any {
                it.matches(
                    source = propertyType,
                    target = target,
                )
            }
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        val valueParam = requireNotNull(source.classDeclaration()?.primaryConstructor?.parameters?.single())

        val propertyName = requireNotNull(valueParam.name).asString()
        val propertyType = valueParam.type.resolve()

        val typeConverter = TypeConverterRegistry.first {
            it.matches(
                source = propertyType,
                target = target,
            )
        }

        return if (needsNotNullAssertionOperator(source, target)) {
            typeConverter.convert("$fieldName!!.$propertyName", propertyType, target)
        } else {
            if (source.isNullable()) {
                CodeBlock.of(
                    "$fieldName?.let { %L }",
                    typeConverter.convert("it.$propertyName", propertyType, target),
                )
            } else {
                typeConverter.convert(
                    "$fieldName.$propertyName",
                    propertyType,
                    target
                )
            }
        }
    }
}
