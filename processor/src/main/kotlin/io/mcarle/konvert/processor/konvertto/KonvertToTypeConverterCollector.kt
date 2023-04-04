package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.api.KonvertTo

object KonvertToTypeConverterCollector {

    fun collect(resolver: Resolver): List<KonvertToTypeConverter> {
        return resolver.getSymbolsWithAnnotation(KonvertTo::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                if (ksClassDeclaration == null || ksClassDeclaration.classKind != ClassKind.CLASS) {
                    throw IllegalStateException("Mapping can only target classes and companion objects")
                }
                if (ksClassDeclaration.typeParameters.isNotEmpty()) {
                    throw IllegalStateException("${KonvertTo::class.simpleName} not allowed on types with generics: $ksAnnotated")
                }

                ksClassDeclaration.annotations
                    .filter { (it.annotationType.toTypeName() as? ClassName)?.canonicalName == KonvertTo::class.qualifiedName }
                    .map {
                        // cannot use getAnnotationsByType, as the KonvertTo.value class may be part of this compilation and
                        // therefore results in ClassNotFoundExceptions when accessing it
                        KonvertToTypeConverter.AnnotationData.from(it)
                    }
                    .map {
                        KonvertToTypeConverter(
                            annotationData = it,
                            sourceClassDeclaration = ksClassDeclaration,
                            targetClassDeclaration = it.value
                        )
                    }
            }.toList()
    }

}
