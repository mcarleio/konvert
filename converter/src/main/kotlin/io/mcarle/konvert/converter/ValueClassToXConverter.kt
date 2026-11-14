package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getDeclaredFunctions
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

            if (sourceClassDeclaration.valueAccessCode(parameter) == null) return@handleNullable false

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
        val sourceClassDeclaration = requireNotNull(source.classDeclaration())
        val valueParam = requireNotNull(sourceClassDeclaration.primaryConstructor?.parameters?.single())

        val accessCode = requireNotNull(sourceClassDeclaration.valueAccessCode(valueParam))
        val propertyType = valueParam.type.resolve()

        val typeConverter = TypeConverterRegistry.first {
            it.matches(
                source = propertyType,
                target = target,
            )
        }

        return if (needsNotNullAssertionOperator(source, target)) {
            typeConverter.convert("$fieldName!!.$accessCode", propertyType, target)
        } else {
            if (source.isNullable()) {
                CodeBlock.of(
                    "$fieldName?.let { %L }",
                    typeConverter.convert("it.$accessCode", propertyType, target),
                )
            } else {
                typeConverter.convert(
                    "$fieldName.$accessCode",
                    propertyType,
                    target
                )
            }
        }
    }

    /**
     * Returns the code to access the underlying value of the value class.
     *
     * If the property is not public, a public Kotlin getter (e.g. `getValue()`) is used instead.
     * This allows value classes like `value class Email private constructor(private val value: String)`
     * which expose their value through a public function.
     */
    private fun KSClassDeclaration.valueAccessCode(parameter: KSValueParameter): String? {
        val parameterName = parameter.name?.asString() ?: return null

        val property = getDeclaredProperties()
            .singleOrNull { it.simpleName.asString() == parameterName }
        if (property?.isPublic() == true) return parameterName

        val parameterType = parameter.type.resolve()
        val getterName = "get${parameterName.replaceFirstChar { it.uppercaseChar() }}"
        return getDeclaredFunctions()
            // TODO: extend TypeConverter to be able to provide more information to use isVisibleFrom here
            .filter { it.parameters.isEmpty() && it.isPublic() }
            .filter { it.returnType?.resolve() == parameterType }
            .firstOrNull { it.simpleName.asString() == getterName }
            ?.let { "$getterName()" }
    }
}

