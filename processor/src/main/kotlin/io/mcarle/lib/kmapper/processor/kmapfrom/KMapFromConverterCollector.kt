package io.mcarle.lib.kmapper.processor.kmapfrom

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.lib.kmapper.api.annotation.KMapFrom

object KMapFromConverterCollector {

    fun collect(resolver: Resolver): List<KMapFromConverter> {
        return resolver.getSymbolsWithAnnotation(KMapFrom::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val annotatedDeclaration = ksAnnotated as? KSClassDeclaration
                    ?: throw IllegalStateException("KMapFrom can only target class declarations or companion objects")

                val (targetKsClassDeclaration, targetCompanionDeclaration) = determineClassAndCompanion(
                    annotatedDeclaration = annotatedDeclaration
                )

                ksAnnotated.annotations
                    .filter { (it.annotationType.toTypeName() as? ClassName)?.canonicalName == KMapFrom::class.qualifiedName }
                    .map {
                        // cannot use getAnnotationsByType, as the KMapFrom.value class may be part of this compilation and
                        // therefore results in ClassNotFoundExceptions when accessing it
                        KMapFromConverter.AnnotationData.from(it)
                    }
                    .map {
                        KMapFromConverter(
                            annotationData = it,
                            sourceClassDeclaration = it.value,
                            targetClassDeclaration = targetKsClassDeclaration,
                            targetCompanionDeclaration = targetCompanionDeclaration
                        )
                    }
            }.toList()
    }

    private fun determineClassAndCompanion(annotatedDeclaration: KSClassDeclaration): Pair<KSClassDeclaration, KSClassDeclaration> {
        return if (annotatedDeclaration.isCompanionObject) {
            val targetKsClassDeclaration = annotatedDeclaration.parentDeclaration as? KSClassDeclaration
                ?: throw RuntimeException("Parent of $annotatedDeclaration is no class declaration")
            if (targetKsClassDeclaration.classKind != ClassKind.CLASS) {
                throw RuntimeException("Parent of $annotatedDeclaration is not ${ClassKind.CLASS} but is ${targetKsClassDeclaration.classKind}")
            }
            val targetCompanionDeclaration = annotatedDeclaration
            targetKsClassDeclaration to targetCompanionDeclaration
        } else if (annotatedDeclaration.classKind == ClassKind.CLASS) {
            val targetCompanionDeclaration =
                annotatedDeclaration.declarations
                    .firstOrNull { (it as? KSClassDeclaration)?.isCompanionObject ?: false } as? KSClassDeclaration
                    ?: throw RuntimeException("Missing Companion for $annotatedDeclaration")
            val targetKsClassDeclaration = annotatedDeclaration
            targetKsClassDeclaration to targetCompanionDeclaration
        } else {
            throw RuntimeException("KMapFrom only allowed on compantion objects or class declarations with a companion")
        }
    }

}