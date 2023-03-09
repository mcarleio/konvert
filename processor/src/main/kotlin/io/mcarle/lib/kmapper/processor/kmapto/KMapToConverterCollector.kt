package io.mcarle.lib.kmapper.processor.kmapto

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.lib.kmapper.api.annotation.KMapTo

object KMapToConverterCollector {

    fun collect(resolver: Resolver): List<KMapToConverter> {
        return resolver.getSymbolsWithAnnotation(KMapTo::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                if (ksClassDeclaration == null || ksClassDeclaration.classKind != ClassKind.CLASS) {
                    throw IllegalStateException("KMap can only target classes and companion objects")
                }


                ksClassDeclaration.annotations
                    .filter { (it.annotationType.toTypeName() as? ClassName)?.canonicalName == KMapTo::class.qualifiedName }
                    .map {
                        // cannot use getAnnotationsByType, as the KMapTo.value class may be part of this compilation and
                        // therefore results in ClassNotFoundExceptions when accessing it
                        KMapToConverter.AnnotationData.from(it)
                    }
                    .map {
                        KMapToConverter(
                            annotationData = it,
                            sourceClassDeclaration = ksClassDeclaration,
                            targetClassDeclaration = it.value
                        )
                    }
            }.toList()
    }

}