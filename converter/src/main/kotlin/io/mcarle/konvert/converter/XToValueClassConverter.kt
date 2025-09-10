package io.mcarle.konvert.converter

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.isNullable

@AutoService(TypeConverter::class)
class XToValueClassConverter : AbstractTypeConverter() {

    override val enabledByDefault: Boolean = true

    override fun matches(source: KSType, target: KSType): Boolean {
        val targetClassDeclaration = target.classDeclaration() ?: return false
        if (targetClassDeclaration.classKind != ClassKind.CLASS) return false
        if (Modifier.VALUE !in targetClassDeclaration.modifiers) return false

        val sourceForTypeConverter = if (source.isNullable() && target.isNullable()) {
            source.makeNotNullable()
        } else {
            source
        }

        return targetClassDeclaration
            .availableConstructors()
            .any { constructor ->
                val parameterType = constructor.parameters.first().type.resolve()
                TypeConverterRegistry.any {
                    it.matches(
                        source = sourceForTypeConverter,
                        target = parameterType,
                    )
                }
            }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        val targetClassDeclaration = requireNotNull(target.classDeclaration())

        val (constructor, typeConverter) = extractBestConstructor(targetClassDeclaration, source)
        val propertyType = constructor.parameters.first().type.resolve()

        return if (source.isNullable() && target.isNullable()) {
            val propertyConverterCode = typeConverter.convert("it", source.makeNotNullable(), propertyType)
            CodeBlock.of(
                "$fieldName?.let { %T(%L) }",
                targetClassDeclaration.toClassName(),
                propertyConverterCode
            )
        } else {
            val propertyConverterCode = typeConverter.convert(fieldName, source, propertyType)
            CodeBlock.of(
                "%T(%L)",
                targetClassDeclaration.toClassName(),
                propertyConverterCode
            )
        }
    }

    private fun KSClassDeclaration.availableConstructors(): Sequence<KSFunctionDeclaration> {
        // TODO: extend TypeConverter to be able to provide more information to use isVisibleFrom here
        return this
            .getConstructors()
            .filter { it.isPublic() } // verify accessible
            .filter { it.parameters.size == 1 } // only single parameter constructors
    }


    /**
     * extracts the best matching constructor based on the registered TypeConverters priorities
     */
    private fun extractBestConstructor(
        targetClassDeclaration: KSClassDeclaration,
        source: KSType
    ): Pair<KSFunctionDeclaration, TypeConverter> {
        return targetClassDeclaration
            .availableConstructors()
            .associateWith { constructor ->
                val parameterType = constructor.parameters.first().type.resolve()
                TypeConverterRegistry.firstOrNull {
                    it.matches(
                        source = source,
                        target = parameterType,
                    )
                }
            }
            .filterValueNotNull()
            .minBy { it.second.priority }
    }

    private fun <K, V> Map<out K, V?>.filterValueNotNull(): List<Pair<K, V>> {
        return mapNotNull { (k, v) -> v?.let { k to it } }
    }
}


