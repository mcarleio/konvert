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
import io.mcarle.konvert.converter.api.isNullable
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

    private val targetTypeNotNullable: KSType = targetType.makeNotNullable()

    // different and more complex handling than others, as the converter's types here can be nullable themselves
    override fun matches(source: KSType, target: KSType): Boolean {
        return handleNullable(source, target) { sourceNotNullable, _ ->
            if (!sourceType.isAssignableFrom(sourceNotNullable)) {
                // cannot pass the current source (ignoring its nullability) to the converter
                return@handleNullable false
            }

            if (!target.isAssignableFrom(targetTypeNotNullable)) {
                // cannot assign the output of the converter (ignoring its nullability) to the required target type
                return@handleNullable false
            }

            true
        }
    }

    override fun needsNotNullAssertionOperator(source: KSType, target: KSType): Boolean {
        if (source.isNullable() && !sourceType.isNullable() && !target.isNullable()) {
            // as the converter expects a not null source value but the actual source type might be null
            // the result of this converter will also be nullable. If the target is not nullable, then
            // a not null assertion operator is needed
            return true
        }

        if (targetType.isNullable() && !target.isNullable()) {
            // the output of the converter is nullable, but the target is not nullable, therefore
            // it would need a not null assertion operator
            return true
        }

        return false
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val getKonverterCode = "${Konverter::class.qualifiedName}.get<${mapKSClassDeclaration.qualifiedName?.asString()}>()"
        val mappingCode = if (source.isNullable() && !sourceType.isNullable()) {
            "$fieldName?.let·{ $getKonverterCode.$mapFunctionName($paramName·=·it) }"
        } else {
            "$getKonverterCode.$mapFunctionName($paramName·=·$fieldName)"
        }
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
