package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.symbol.KSAnnotation
import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.konverterGenerateClass
import io.mcarle.konvert.converter.api.config.withIsolatedConfiguration
import io.mcarle.konvert.processor.AnnotatedConverter
import io.mcarle.konvert.processor.AnnotatedConverterData
import io.mcarle.konvert.processor.from

class KonverterData constructor(
    val annotationData: AnnotationData,
    val konvertData: List<KonvertData>,
    val konverterInterface: KonverterInterface
) : AnnotatedConverterData {

    override fun toTypeConverters(): List<AnnotatedConverter> {
        return withIsolatedConfiguration(annotationData.options) {
            // a function with parameters beyond its source cannot be called with the single value a type converter passes
            konvertData.filter { it.additionalParameters.isEmpty() }.map {
                KonvertTypeConverter(
                    priority = it.priority,
                    alreadyGenerated = false,
                    sourceType = it.sourceType,
                    targetType = it.targetType,
                    mapFunctionName = it.mapFunctionName,
                    paramName = it.paramName,
                    konverterInterface = konverterInterface,
                    classKind = if (Configuration.konverterGenerateClass)
                        KonvertTypeConverter.ClassOrObject.CLASS
                    else
                        KonvertTypeConverter.ClassOrObject.OBJECT
                )
            }
        }
    }

    data class AnnotationData(
        val options: List<Konfig>
    ) {

        companion object {
            fun from(annotation: KSAnnotation) = AnnotationData(
                options = (annotation.arguments.first { it.name?.asString() == Konverter::options.name }.value as List<*>)
                    .filterIsInstance<KSAnnotation>()
                    .map { Konfig.from(it) },
            )
        }

    }
}
