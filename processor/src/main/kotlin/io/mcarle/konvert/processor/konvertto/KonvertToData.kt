package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.api.DEFAULT_KONVERT_TO_PRIORITY
import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.classDeclaration
import com.google.devtools.ksp.processing.Resolver
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.AnnotatedConverterData
import io.mcarle.konvert.processor.argumentValue
import io.mcarle.konvert.processor.constructorArgClassDeclarations
import io.mcarle.konvert.processor.from

class KonvertToData(
    val annotationData: AnnotationData,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration
) : AnnotatedConverterData {

    val mapFunctionName: String = annotationData.mapFunctionName.ifEmpty { "to${targetClassDeclaration.toClassName().simpleName}" }
    val priority = annotationData.priority


    override fun toTypeConverters(): List<AnnotatedConverter> {
        return listOf(
            KonvertToTypeConverter(
                priority = priority,
                alreadyGenerated = false,
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
        val priority: Priority,
        val options: List<Konfig>
    ) {

        companion object {
            fun from(annotation: KSAnnotation, resolver: Resolver) = AnnotationData(
                value = (annotation.argumentValue(KonvertTo::value.name) as KSType).classDeclaration()!!,
                mappings = (annotation.argumentValue(KonvertTo::mappings.name, emptyList<Any?>()) as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { Mapping.from(it) },
                constructor = annotation.constructorArgClassDeclarations(KonvertTo::constructorArgs.name, resolver),
                mapFunctionName = annotation.argumentValue(KonvertTo::mapFunctionName.name, "") as String,
                priority = annotation.argumentValue(KonvertTo::priority.name, DEFAULT_KONVERT_TO_PRIORITY) as Priority,
                options = (annotation.argumentValue(KonvertTo::options.name, emptyList<Any?>()) as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { Konfig.from(it) },
            )
        }

    }
}
