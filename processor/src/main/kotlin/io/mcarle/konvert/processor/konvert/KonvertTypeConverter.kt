package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.AbstractTypeConverter
import io.mcarle.konvert.converter.api.DEFAULT_KONVERTER_PRIORITY
import io.mcarle.konvert.converter.api.Priority
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.from

class KonvertTypeConverter constructor(
    val annotation: AnnotationData?,
    val sourceClassDeclaration: KSClassDeclaration,
    val sourceType: KSType,
    val targetClassDeclaration: KSClassDeclaration,
    val targetType: KSType,
    val mapKSClassDeclaration: KSClassDeclaration,
    val mapKSFunctionDeclaration: KSFunctionDeclaration,
) : AbstractTypeConverter(), AnnotatedConverter {

    val mapFunctionName: String = mapKSFunctionDeclaration.simpleName.asString()
    val paramName: String = mapKSFunctionDeclaration.parameters.first().name!!.asString()

    override val enabledByDefault: Boolean = true
    override val priority: Priority = annotation?.priority ?: DEFAULT_KONVERTER_PRIORITY

    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { _, targetNotNullable ->
            // the source type must be assignable (e.g. String? and String are assignable from String)
            sourceType.isAssignableFrom(source) && targetType == targetNotNullable
        }
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val getKonverterCode = "${Konverter::class.qualifiedName}.get<${mapKSClassDeclaration.qualifiedName?.asString()}>()"
        val mappingCode = "$getKonverterCode.$mapFunctionName($paramName·=·$fieldName)"
        return mappingCode + appendNotNullAssertionOperatorIfNeeded(source, target)
    }

    data class AnnotationData(
        val mappings: List<Mapping>,
        val constructor: List<KSClassDeclaration>,
        val priority: Priority
    ) {

        companion object {
            fun from(annotation: KSAnnotation) = AnnotationData(
                mappings = (annotation.arguments.first { it.name?.asString() == Konvert::mappings.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { Mapping.from(it) },
                constructor = (annotation.arguments.first { it.name?.asString() == Konvert::constructor.name }.value as List<*>).mapNotNull { (it as? KSType)?.classDeclaration() },
                priority = annotation.arguments.first { it.name?.asString() == Konvert::priority.name }.value as Priority,
            )

            fun default(resolver: Resolver) = with(Konvert()) {
                AnnotationData(
                    mappings = this.mappings.toList(),
                    constructor = this.constructor.mapNotNull { resolver.getClassDeclarationByName(it.qualifiedName!!) },
                    priority = this.priority,
                )
            }
        }

    }

}
