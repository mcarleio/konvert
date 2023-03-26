package io.mcarle.konvert.processor.konvertfrom

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.Priority
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.from
import java.util.Locale

class KonvertFromTypeConverter(
    val annotationData: AnnotationData,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val targetCompanionDeclaration: KSClassDeclaration,
) : TypeConverter, io.mcarle.konvert.processor.AnnotatedConverter {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override val enabledByDefault: Boolean = true
    override val priority: Priority = annotationData.priority
    val mapFunctionName: String = annotationData.mapFunctionName.ifEmpty { "from${sourceClassDeclaration.simpleName.asString()}" }
    val paramName: String = sourceClassDeclaration.simpleName.asString().replaceFirstChar { it.lowercase(Locale.getDefault()) }

    override fun init(config: io.mcarle.konvert.converter.api.ConverterConfig) {
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
        return if (source.isNullable()) {
            return "$fieldName?.let·{ ${targetClassDeclaration.qualifiedName?.asString()}.$mapFunctionName($paramName·=·it) }"
        } else {
            "${targetClassDeclaration.qualifiedName?.asString()}.$mapFunctionName($paramName·=·$fieldName)"
        }
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
                value = (annotation.arguments.first { it.name?.asString() == KonvertFrom::value.name }.value as KSType).declaration as KSClassDeclaration,
                mappings = (annotation.arguments.first { it.name?.asString() == KonvertFrom::mappings.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { Mapping.from(it) },
                constructor = (annotation.arguments.first { it.name?.asString() == KonvertFrom::constructor.name }.value as List<*>).mapNotNull { (it as? KSType)?.declaration as? KSClassDeclaration },
                mapFunctionName = annotation.arguments.first { it.name?.asString() == KonvertFrom::mapFunctionName.name }.value as String,
                priority = annotation.arguments.first { it.name?.asString() == KonvertFrom::priority.name }.value as Priority,
            )
        }
    }

}
