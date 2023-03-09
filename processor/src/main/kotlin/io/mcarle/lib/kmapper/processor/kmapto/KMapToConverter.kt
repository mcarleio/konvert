package io.mcarle.lib.kmapper.processor.kmapto

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.api.annotation.KMapTo
import io.mcarle.lib.kmapper.converter.api.ConverterConfig
import io.mcarle.lib.kmapper.converter.api.Priority
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import io.mcarle.lib.kmapper.converter.api.isNullable
import io.mcarle.lib.kmapper.processor.shared.AnnotatedConverter
import io.mcarle.lib.kmapper.processor.shared.from

class KMapToConverter(
    val annotationData: AnnotationData,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration
) : TypeConverter, AnnotatedConverter {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override val enabledByDefault: Boolean = true
    override val priority: Priority = annotationData.priority
    val mapFunctionName: String = annotationData.mapFunctionName.ifEmpty { "mapTo${targetClassDeclaration.toClassName().simpleName}" }

    override fun init(config: ConverterConfig) {
        // Nothing to initialize
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return sourceType in setOf(
            source,
            source.makeNotNullable()
        ) && targetType in setOf(
            target,
            target.makeNotNullable()
        )
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val nc = if (source.isNullable()) "?" else ""
        return "$fieldName$nc.$mapFunctionName()"
    }

    data class AnnotationData(
        val value: KSClassDeclaration,
        val mappings: List<KMap>,
        val mapFunctionName: String,
        val priority: Priority
    ) {

        companion object {
            fun from(annotation: KSAnnotation) = AnnotationData(
                value = (annotation.arguments.first { it.name?.asString() == KMapTo::value.name }.value as KSType).declaration as KSClassDeclaration,
                mappings = (annotation.arguments.first { it.name?.asString() == KMapTo::mappings.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { KMap.from(it) },
                mapFunctionName = annotation.arguments.first { it.name?.asString() == KMapTo::mapFunctionName.name }.value as String,
                priority = annotation.arguments.first { it.name?.asString() == KMapTo::priority.name }.value as Priority,
            )
        }

    }
}
