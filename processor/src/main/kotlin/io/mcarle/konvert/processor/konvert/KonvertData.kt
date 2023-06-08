package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import io.mcarle.konvert.api.DEFAULT_KONVERTER_PRIORITY
import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.processor.from

class KonvertData(
    val annotationData: AnnotationData?,
    val sourceTypeReference: KSTypeReference,
    val targetTypeReference: KSTypeReference,
    val mapKSFunctionDeclaration: KSFunctionDeclaration,
) {

    val sourceType: KSType = sourceTypeReference.resolve()
    val sourceClassDeclaration: KSClassDeclaration = sourceType.classDeclaration()!!
    val targetType: KSType = targetTypeReference.resolve()
    val targetClassDeclaration: KSClassDeclaration = targetType.classDeclaration()!!
    val mapFunctionName: String = mapKSFunctionDeclaration.simpleName.asString()
    val paramName: String = mapKSFunctionDeclaration.parameters.first().name!!.asString()

    val priority = annotationData?.priority ?: DEFAULT_KONVERTER_PRIORITY

    data class AnnotationData(
        val mappings: List<Mapping>,
        val constructor: List<KSClassDeclaration>,
        val priority: Priority,
        val options: List<Konfig>
    ) {

        companion object {
            fun from(annotation: KSAnnotation) = AnnotationData(
                mappings = (annotation.arguments.first { it.name?.asString() == Konvert::mappings.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { Mapping.from(it) },
                constructor = (annotation.arguments.first { it.name?.asString() == Konvert::constructor.name }.value as List<*>).mapNotNull { (it as? KSType)?.classDeclaration() },
                priority = annotation.arguments.first { it.name?.asString() == Konvert::priority.name }.value as Priority,
                options = (annotation.arguments.first { it.name?.asString() == Konvert::options.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { Konfig.from(it) },
            )

            fun default(resolver: Resolver) = with(Konvert()) {
                AnnotationData(
                    mappings = this.mappings.toList(),
                    constructor = this.constructor.mapNotNull { resolver.getClassDeclarationByName(it.qualifiedName!!) },
                    priority = this.priority,
                    options = emptyList()
                )
            }
        }

    }

}
