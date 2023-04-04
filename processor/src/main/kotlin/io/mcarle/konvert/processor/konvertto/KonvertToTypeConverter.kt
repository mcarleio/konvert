package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.Priority
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.from

class KonvertToTypeConverter(
    val annotationData: AnnotationData,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration
) : AbstractTypeConverter(), AnnotatedConverter {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override val enabledByDefault: Boolean = true
    override val priority: Priority = annotationData.priority
    val mapFunctionName: String = annotationData.mapFunctionName.ifEmpty { "to${targetClassDeclaration.toClassName().simpleName}" }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceType == sourceNotNullable && targetType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val nc = if (source.isNullable()) "?" else ""
        return "$fieldName$nc.$mapFunctionName()" + appendNotNullAssertionOperatorIfNeeded(source, target)
    }

    data class AnnotationData(
        val value: KSClassDeclaration,
        val mappings: List<Mapping>,
        val constructor: List<KSClassDeclaration>,
        val mapFunctionName: String,
        val priority: Priority
    ) {

        companion object {
            fun from(annotation: KSAnnotation) = AnnotationData(
                value = (annotation.arguments.first { it.name?.asString() == KonvertTo::value.name }.value as KSType).classDeclaration()!!,
                mappings = (annotation.arguments.first { it.name?.asString() == KonvertTo::mappings.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { Mapping.from(it) },
                constructor = (annotation.arguments.first { it.name?.asString() == KonvertTo::constructor.name }.value as List<*>).mapNotNull { (it as? KSType)?.classDeclaration() },
                mapFunctionName = annotation.arguments.first { it.name?.asString() == KonvertTo::mapFunctionName.name }.value as String,
                priority = annotation.arguments.first { it.name?.asString() == KonvertTo::priority.name }.value as Priority,
            )
        }

    }
}
