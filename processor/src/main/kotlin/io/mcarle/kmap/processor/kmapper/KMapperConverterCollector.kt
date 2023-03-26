package io.mcarle.kmap.processor.kmapper

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.kmap.api.annotation.KMapper
import io.mcarle.kmap.api.annotation.KMapping

object KMapperConverterCollector {

    fun collect(resolver: Resolver, logger: KSPLogger): List<KMapperConverter> {
        return resolver.getSymbolsWithAnnotation(KMapper::class.qualifiedName!!)
            .flatMap { ksAnnotated ->
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                if (ksClassDeclaration == null || ksClassDeclaration.classKind != ClassKind.INTERFACE) {
                    throw IllegalStateException("KMap can only target interfaces")
                }

                ksClassDeclaration
                    .getAllFunctions()
                    .mapNotNull {
                        if (it.simpleName.asString() in arrayOf("equals", "toString", "hashCode")) {
                            // ignore standard functions
                            return@mapNotNull null
                        }

                        val source =
                            if (it.parameters.size > 1 || it.parameters.isEmpty()) null
                            else it.parameters.first().type.resolve().declaration as? KSClassDeclaration
                        val target = it.returnType?.resolve()?.declaration as? KSClassDeclaration

                        val annotation = it.annotations.firstOrNull { annotation ->
                            (annotation.annotationType.toTypeName() as? ClassName)?.canonicalName == KMapping::class.qualifiedName
                        }?.let { annotation ->
                            // cannot use getAnnotationsByType, as the KMapping.constructor classes may be part of this compilation and
                            // therefore results in ClassNotFoundExceptions when accessing it
                            KMapperConverter.AnnotationData.from(annotation)
                        }

                        if (annotation != null && it.isAbstract) {
                            if (source == null || target == null) {
                                throw IllegalStateException("KMapping annotated function must have exactly one parameter and must have a return type: $it")
                            }

                            KMapperConverter(
                                annotation = annotation,
                                sourceClassDeclaration = source,
                                targetClassDeclaration = target,
                                mapKSClassDeclaration = ksClassDeclaration,
                                mapKSFunctionDeclaration = it
                            )
                        } else if (source != null && target != null) {
                            KMapperConverter(
                                annotation = if (it.isAbstract) KMapperConverter.AnnotationData.default(resolver) else null,
                                sourceClassDeclaration = source,
                                targetClassDeclaration = target,
                                mapKSClassDeclaration = ksClassDeclaration,
                                mapKSFunctionDeclaration = it
                            )
                        } else if (it.isAbstract) {
                            throw RuntimeException("Method $it is abstract and does not meet criteria for automatic source and target detection")
                        } else {
                            logger.warn("Could not determine source and/or target", it)
                            null
                        }
                    }.toList()
            }.toList()
    }

}