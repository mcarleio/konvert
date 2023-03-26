package io.mcarle.kmap.processor.kmapper

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.kmap.api.KMappers
import io.mcarle.kmap.api.annotation.KMap
import io.mcarle.kmap.api.annotation.KMapping
import io.mcarle.kmap.converter.api.ConverterConfig
import io.mcarle.kmap.converter.api.DEFAULT_KMAPPER_NO_ANNOTATION_PRIORITY
import io.mcarle.kmap.converter.api.Priority
import io.mcarle.kmap.converter.api.TypeConverter
import io.mcarle.kmap.processor.from

class KMapperConverter constructor(
    val annotation: AnnotationData?,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val mapKSClassDeclaration: KSClassDeclaration,
    val mapKSFunctionDeclaration: KSFunctionDeclaration,
) : TypeConverter, io.mcarle.kmap.processor.AnnotatedConverter {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    val mapFunctionName: String = mapKSFunctionDeclaration.simpleName.asString()
    val paramName: String = mapKSFunctionDeclaration.parameters.first().name!!.asString()

    override val enabledByDefault: Boolean = true
    override val priority: Priority = annotation?.priority ?: DEFAULT_KMAPPER_NO_ANNOTATION_PRIORITY

    override fun init(config: ConverterConfig) {
        // Nothing to initialize
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return sourceType == source && targetType == target
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return "${KMappers::class.qualifiedName}.get<${mapKSClassDeclaration.qualifiedName?.asString()}>().$mapFunctionName($paramName·=·$fieldName)"
    }

    data class AnnotationData(
        val mappings: List<KMap>,
        val constructor: List<KSClassDeclaration>,
        val priority: Priority
    ) {

        companion object {
            fun from(annotation: KSAnnotation) = AnnotationData(
                mappings = (annotation.arguments.first { it.name?.asString() == KMapping::mappings.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { KMap.from(it) },
                constructor = (annotation.arguments.first { it.name?.asString() == KMapping::constructor.name }.value as List<*>).mapNotNull { (it as? KSType)?.declaration as? KSClassDeclaration },
                priority = annotation.arguments.first { it.name?.asString() == KMapping::priority.name }.value as Priority,
            )

            fun default(resolver: Resolver) = with(KMapping()) {
                AnnotationData(
                    mappings = this.mappings.toList(),
                    constructor = this.constructor.mapNotNull { resolver.getClassDeclarationByName(it.qualifiedName!!) },
                    priority = this.priority,
                )
            }
        }

    }

}