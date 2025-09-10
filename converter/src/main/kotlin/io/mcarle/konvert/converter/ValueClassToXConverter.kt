package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
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

            val primaryConstructor = sourceClassDeclaration.primaryConstructor ?: return@handleNullable false
            val parameter = primaryConstructor.parameters.singleOrNull() ?: return@handleNullable false

            if (!sourceClassDeclaration.isPropertyAccessible(parameter)) return@handleNullable false

            val parameterType = parameter.type.resolve()
            return@handleNullable TypeConverterRegistry.any {
                it.matches(
                    source = parameterType,
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

    private fun KSClassDeclaration.isPropertyAccessible(parameter: KSValueParameter): Boolean {
        return this.getDeclaredProperties()
            .single { it.simpleName == parameter.name }
            .isPublic() // TODO: extend TypeConverter#matches to be able to provide more information to use isVisibleFrom here
    }
}

