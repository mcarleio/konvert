package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter

object KonverterDataCollector {

    fun collect(resolver: Resolver, logger: KSPLogger): List<KonverterData> {
        return resolver.getSymbolsWithAnnotation(Konverter::class.qualifiedName!!)
            .map { ksAnnotated ->
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                if (ksClassDeclaration == null || ksClassDeclaration.classKind != ClassKind.INTERFACE) {
                    throw IllegalStateException("Mapping can only target interfaces")
                }

                val annotation = ksClassDeclaration.annotations.first { annotation ->
                    (annotation.annotationType.toTypeName() as? ClassName)?.canonicalName == Konverter::class.qualifiedName
                }.let { annotation ->
                    KonverterData.AnnotationData.from(annotation)
                }

                KonverterData(
                    annotationData = annotation,
                    konvertData = collectKonvertData(ksClassDeclaration, resolver, logger),
                    ksClassDeclaration
                )

            }.toList()
    }

    private fun collectKonvertData(ksClassDeclaration: KSClassDeclaration, resolver: Resolver, logger: KSPLogger): List<KonvertData> {
        return ksClassDeclaration
            .getAllFunctions()
            .mapNotNull {
                if (it.simpleName.asString() in arrayOf("equals", "toString", "hashCode")) {
                    // ignore standard functions
                    return@mapNotNull null
                }

                val source =
                    if (it.parameters.size > 1 || it.parameters.isEmpty()) null
                    else it.parameters.first().type
                val target = it.returnType

                val annotation = it.annotations.firstOrNull { annotation ->
                    (annotation.annotationType.toTypeName() as? ClassName)?.canonicalName == Konvert::class.qualifiedName
                }?.let { annotation ->
                    // cannot use getAnnotationsByType, as the Konvert.constructor classes may be part of this compilation and
                    // therefore results in ClassNotFoundExceptions when accessing it
                    KonvertData.AnnotationData.from(annotation)
                }

                if (annotation != null && it.isAbstract) {
                    if (source == null || target == null) {
                        throw IllegalStateException("Konvert annotated function must have exactly one parameter and must have a return type: $it")
                    }

                    KonvertData(
                        annotationData = annotation,
                        isAbstract = true,
                        sourceTypeReference = source,
                        targetTypeReference = target,
                        mapKSFunctionDeclaration = it
                    )
                } else if (source != null && target != null) {
                    KonvertData(
                        annotationData = annotation ?: KonvertData.AnnotationData.default(resolver, it.isAbstract),
                        isAbstract = it.isAbstract,
                        sourceTypeReference = source,
                        targetTypeReference = target,
                        mapKSFunctionDeclaration = it
                    )
                } else if (it.isAbstract) {
                    throw RuntimeException("Method $it is abstract and does not meet criteria for automatic source and target detection")
                } else {
                    logger.warn("Could not determine source and/or target", it)
                    null
                }
            }.toList()
    }

}
