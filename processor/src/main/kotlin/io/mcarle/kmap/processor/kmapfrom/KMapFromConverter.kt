package io.mcarle.kmap.processor.kmapfrom

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.kmap.api.annotation.KMap
import io.mcarle.kmap.api.annotation.KMapFrom
import io.mcarle.kmap.converter.api.ConverterConfig
import io.mcarle.kmap.converter.api.Priority
import io.mcarle.kmap.converter.api.TypeConverter
import io.mcarle.kmap.converter.api.isNullable
import io.mcarle.kmap.processor.from
import java.util.Locale

class KMapFromConverter(
    val annotationData: AnnotationData,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val targetCompanionDeclaration: KSClassDeclaration,
) : TypeConverter, io.mcarle.kmap.processor.AnnotatedConverter {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override val enabledByDefault: Boolean = true
    override val priority: Priority = annotationData.priority
    val mapFunctionName: String = annotationData.mapFunctionName.ifEmpty { "from${sourceClassDeclaration.simpleName.asString()}" }
    val paramName: String = sourceClassDeclaration.simpleName.asString().replaceFirstChar { it.lowercase(Locale.getDefault()) }

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
        return if (source.isNullable()) {
            return "$fieldName?.let·{ ${targetClassDeclaration.qualifiedName?.asString()}.$mapFunctionName($paramName·=·it) }"
        } else {
            "${targetClassDeclaration.qualifiedName?.asString()}.$mapFunctionName($paramName·=·$fieldName)"
        }
    }

    data class AnnotationData(
        val value: KSClassDeclaration,
        val mappings: List<KMap>,
        val constructor: List<KSClassDeclaration>,
        val mapFunctionName: String,
        val priority: Priority
    ) {

        companion object {
            fun from(annotation: KSAnnotation) = AnnotationData(
                value = (annotation.arguments.first { it.name?.asString() == KMapFrom::value.name }.value as KSType).declaration as KSClassDeclaration,
                mappings = (annotation.arguments.first { it.name?.asString() == KMapFrom::mappings.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { KMap.from(it) },
                constructor = (annotation.arguments.first { it.name?.asString() == KMapFrom::constructor.name }.value as List<*>).mapNotNull { (it as? KSType)?.declaration as? KSClassDeclaration },
                mapFunctionName = annotation.arguments.first { it.name?.asString() == KMapFrom::mapFunctionName.name }.value as String,
                priority = annotation.arguments.first { it.name?.asString() == KMapFrom::priority.name }.value as Priority,
            )
        }
    }

}
