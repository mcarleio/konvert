package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.api.KonvertTo

object KonvertToDataCollector {

    fun collect(resolver: Resolver, logger: KSPLogger): List<KonvertToData> {
        return resolver.getSymbolsWithAnnotation(KonvertTo::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                check(ksClassDeclaration != null && ksClassDeclaration.classKind == ClassKind.CLASS) {
                    "${KonvertTo::class.simpleName} can only target classes and companion objects"
                }
                check(ksClassDeclaration.typeParameters.isEmpty()) {
                    "${KonvertTo::class.simpleName} not allowed on types with generics: $ksAnnotated"
                }

                ksClassDeclaration.annotations
                    .filter { (it.annotationType.toTypeName() as? ClassName)?.canonicalName == KonvertTo::class.qualifiedName }
                    .map {
                        // cannot use getAnnotationsByType, as the KonvertTo.value class may be part of this compilation and
                        // therefore results in ClassNotFoundExceptions when accessing it
                        KonvertToData.AnnotationData.from(it)
                    }
                    .map {
                        KonvertToData(
                            annotationData = it,
                            sourceClassDeclaration = ksClassDeclaration,
                            targetClassDeclaration = it.value
                        )
                    }
            }.toList()
    }

}
