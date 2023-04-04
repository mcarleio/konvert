package io.mcarle.konvert.processor.konvertfrom

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.Priority
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.from
import java.util.Locale

class KonvertFromTypeConverter(
    val annotationData: AnnotationData,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val targetCompanionDeclaration: KSClassDeclaration,
) : AbstractTypeConverter(), AnnotatedConverter {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override val enabledByDefault: Boolean = true
    override val priority: Priority = annotationData.priority
    val mapFunctionName: String = annotationData.mapFunctionName.ifEmpty { "from${sourceClassDeclaration.simpleName.asString()}" }
    val paramName: String = sourceClassDeclaration.simpleName.asString().replaceFirstChar { it.lowercase(Locale.getDefault()) }

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, targetNotNullable ->
            sourceType == sourceNotNullable && targetType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return if (source.isNullable()) {
            "$fieldName?.let·{ ${targetClassDeclaration.qualifiedName?.asString()}.$mapFunctionName($paramName·=·it) }"
        } else {
            "${targetClassDeclaration.qualifiedName?.asString()}.$mapFunctionName($paramName·=·$fieldName)"
        } + appendNotNullAssertionOperatorIfNeeded(source, target)
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
                value = (annotation.arguments.first { it.name?.asString() == KonvertFrom::value.name }.value as KSType).classDeclaration()!!,
                mappings = (annotation.arguments.first { it.name?.asString() == KonvertFrom::mappings.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { Mapping.from(it) },
                constructor = (annotation.arguments.first { it.name?.asString() == KonvertFrom::constructor.name }.value as List<*>).mapNotNull { (it as? KSType)?.classDeclaration() },
                mapFunctionName = annotation.arguments.first { it.name?.asString() == KonvertFrom::mapFunctionName.name }.value as String,
                priority = annotation.arguments.first { it.name?.asString() == KonvertFrom::priority.name }.value as Priority,
            )
        }
    }

}
