package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.Priority
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.processor.AnnotatedConverterData
import io.mcarle.konvert.processor.from

class KonvertToData(
    val annotationData: AnnotationData,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration
) : AnnotatedConverterData {

    val mapFunctionName: String = annotationData.mapFunctionName.ifEmpty { "to${targetClassDeclaration.toClassName().simpleName}" }

    override fun toTypeConverters(): List<TypeConverter> {
        return listOf(
            KonvertToTypeConverter(
                priority = annotationData.priority,
                mapFunctionName = mapFunctionName,
                sourceClassDeclaration = sourceClassDeclaration,
                targetClassDeclaration = targetClassDeclaration
            )
        )
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
