package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.converter.api.DEFAULT_KONVERTER_PRIORITY
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.processor.AnnotatedConverterData
import io.mcarle.konvert.processor.from

class KonverterData(
    val annotationData: AnnotationData,
    val konvertData: List<KonvertData>,
    val mapKSClassDeclaration: KSClassDeclaration
) : AnnotatedConverterData {

    override fun toTypeConverters(): List<TypeConverter> {
        return konvertData.map {
            KonvertTypeConverter(
                priority = it.annotationData?.priority ?: DEFAULT_KONVERTER_PRIORITY,
                sourceType = it.sourceType,
                targetType = it.targetType,
                mapFunctionName = it.mapFunctionName,
                paramName = it.paramName,
                mapKSClassDeclaration = mapKSClassDeclaration
            )
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

            fun default(resolver: Resolver) = with(Konvert()) {
                AnnotationData(
                    options = emptyList()
                )
            }
        }

    }
}
