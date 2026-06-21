package io.mcarle.konvert.processor.konvertfrom

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.konvert.api.DEFAULT_KONVERT_FROM_PRIORITY
import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.AnnotatedConverterData
import io.mcarle.konvert.processor.argumentValue
import io.mcarle.konvert.processor.constructorArgClassDeclarations
import io.mcarle.konvert.processor.from
import java.util.Locale

class KonvertFromData(
    val annotationData: AnnotationData,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val targetCompanionDeclaration: KSClassDeclaration,
) : AnnotatedConverterData {

    val mapFunctionName: String = annotationData.mapFunctionName.ifEmpty { "from${sourceClassDeclaration.simpleName.asString()}" }
    val paramName: String = sourceClassDeclaration.simpleName.asString().replaceFirstChar { it.lowercase(Locale.getDefault()) }

    val priority = annotationData.priority

    override fun toTypeConverters(): List<AnnotatedConverter> {
        return listOf(
            KonvertFromTypeConverter(
                priority = priority,
                alreadyGenerated = false,
                mapFunctionName = mapFunctionName,
                paramName = paramName,
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
            fun from(annotation: KSAnnotation, unitType: KSType) = AnnotationData(
                value = (annotation.argumentValue<KSType>(KonvertFrom::value.name)).classDeclaration()!!,
                mappings = (annotation.argumentValue(KonvertFrom::mappings.name, emptyList<Any?>()))
                    .filterIsInstance<KSAnnotation>()
                    .map { Mapping.from(it) },
                constructor = annotation.constructorArgClassDeclarations(KonvertFrom::constructorArgs.name, unitType),
                mapFunctionName = annotation.argumentValue(KonvertFrom::mapFunctionName.name, ""),
                priority = annotation.argumentValue(KonvertFrom::priority.name, DEFAULT_KONVERT_FROM_PRIORITY),
                options = (annotation.argumentValue(KonvertFrom::options.name, emptyList<Any?>()))
                    .filterIsInstance<KSAnnotation>()
                    .map { Konfig.from(it) },
            )
        }
    }

}
