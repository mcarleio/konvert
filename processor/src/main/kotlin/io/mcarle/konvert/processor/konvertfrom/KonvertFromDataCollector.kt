package io.mcarle.konvert.processor.konvertfrom

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.api.KonvertFrom

object KonvertFromDataCollector {

    fun collect(resolver: Resolver, logger: KSPLogger): List<KonvertFromData> {
        return resolver.getSymbolsWithAnnotation(KonvertFrom::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val annotatedDeclaration = ksAnnotated as? KSClassDeclaration
                val (targetKsClassDeclaration, targetCompanionDeclaration) = determineClassAndCompanion(
                    annotatedDeclaration = annotatedDeclaration
                )

                check(targetKsClassDeclaration.typeParameters.isEmpty()) {
                    "@${KonvertFrom::class.simpleName} not allowed on classes with generics: ${targetKsClassDeclaration.qualifiedName?.asString() ?: targetKsClassDeclaration}"
                }

                ksAnnotated.annotations
                    .filter { (it.annotationType.toTypeName() as? ClassName)?.canonicalName == KonvertFrom::class.qualifiedName }
                    .map {
                        // cannot use getAnnotationsByType, as the KonvertFrom.value class may be part of this compilation and
                        // therefore results in ClassNotFoundExceptions when accessing it
                        KonvertFromData.AnnotationData.from(it)
                    }
                    .map {
                        KonvertFromData(
                            annotationData = it,
                            sourceClassDeclaration = it.value,
                            targetClassDeclaration = targetKsClassDeclaration,
                            targetCompanionDeclaration = targetCompanionDeclaration
                        )
                    }
            }.toList()
    }

    private fun determineClassAndCompanion(annotatedDeclaration: KSClassDeclaration?): Pair<KSClassDeclaration, KSClassDeclaration> {
        return if (annotatedDeclaration?.isCompanionObject == true) {
            val targetKsClassDeclaration = annotatedDeclaration.parentDeclaration as? KSClassDeclaration
            check(targetKsClassDeclaration != null && targetKsClassDeclaration.classKind == ClassKind.CLASS) {
                "Parent of ${annotatedDeclaration.qualifiedName?.asString() ?: annotatedDeclaration} " +
                    "is not a class: ${targetKsClassDeclaration?.qualifiedName?.asString() ?: targetKsClassDeclaration}"
            }
            targetKsClassDeclaration to annotatedDeclaration
        } else if (annotatedDeclaration?.classKind == ClassKind.CLASS) {
            val targetCompanionDeclaration =
                annotatedDeclaration.declarations
                    .firstOrNull { (it as? KSClassDeclaration)?.isCompanionObject ?: false } as? KSClassDeclaration
                    ?: error("Missing companion in ${annotatedDeclaration.qualifiedName?.asString() ?: annotatedDeclaration}")
            annotatedDeclaration to targetCompanionDeclaration
        } else {
            error("@${KonvertFrom::class.simpleName} only allowed on companion objects or class declarations with a companion, but ${annotatedDeclaration?.qualifiedName?.asString() ?: annotatedDeclaration} is neither")
        }
    }

}
