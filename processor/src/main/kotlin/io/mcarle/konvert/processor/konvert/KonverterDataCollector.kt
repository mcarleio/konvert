package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter

object KonverterDataCollector {

    fun collect(resolver: Resolver, logger: KSPLogger): List<KonverterData> {
        return resolver.getSymbolsWithAnnotation(Konverter::class.qualifiedName!!)
            .map { ksAnnotated ->
                val ksClassDeclaration = ksAnnotated as? KSClassDeclaration
                check(ksClassDeclaration != null && ksClassDeclaration.classKind == ClassKind.INTERFACE) {
                    "Mapping can only target interfaces"
                }

                val annotation = ksClassDeclaration.annotations.first { annotation ->
                    (annotation.annotationType.toTypeName() as? ClassName)?.canonicalName == Konverter::class.qualifiedName
                }.let { annotation ->
                    KonverterData.AnnotationData.from(annotation)
                }

                KonverterData(
                    annotationData = annotation,
                    konvertData = collectKonvertData(ksClassDeclaration, resolver, logger),
                    konverterInterface = KonverterInterface(ksClassDeclaration)
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

                if (it.isPrivate()) {
                    // ignore private functions
                    return@mapNotNull null
                }

                if (Modifier.INLINE in it.modifiers) {
                    // ignore inline functions
                    return@mapNotNull null
                }

                if (it.extensionReceiver != null) {
                    // ignore extension functions
                    return@mapNotNull null
                }

                val sourceValueParameter = determineSourceParam(it, logger)
                val source = sourceValueParameter?.type
                val target = it.returnType?.let { returnType ->
                    if (returnType.resolve().declaration == resolver.getClassDeclarationByName<Unit>()) {
                        null
                    } else {
                        returnType
                    }
                }

                val annotation = it.annotations.firstOrNull { annotation ->
                    (annotation.annotationType.toTypeName() as? ClassName)?.canonicalName == Konvert::class.qualifiedName
                }?.let { annotation ->
                    // cannot use getAnnotationsByType, as the Konvert.constructor classes may be part of this compilation and
                    // therefore results in ClassNotFoundExceptions when accessing it
                    KonvertData.AnnotationData.from(annotation)
                }

                if (annotation != null && it.isAbstract) {
                    check(source != null && target != null) {
                        "Konvert annotated function must have exactly one parameter and must have a return type: $it"
                    }

                    KonvertData(
                        annotationData = annotation,
                        isAbstract = true,
                        sourceTypeReference = source,
                        targetTypeReference = target,
                        mapKSFunctionDeclaration = it,
                        additionalParameters = determineAdditionalParams(it, sourceValueParameter)
                    )
                } else if (source != null && target != null) {
                    KonvertData(
                        annotationData = annotation ?: KonvertData.AnnotationData.default(resolver, it.isAbstract),
                        isAbstract = it.isAbstract,
                        sourceTypeReference = source,
                        targetTypeReference = target,
                        mapKSFunctionDeclaration = it,
                        additionalParameters = determineAdditionalParams(it, sourceValueParameter)
                    )
                } else if (it.isAbstract) {
                    throw RuntimeException("Method $it is abstract and does not meet criteria for automatic source and target detection")
                } else {
                    if (annotation != null) {
                        logger.warn("Could not determine source and/or target", it)
                    } else {
                        logger.logging("Could not determine source and/or target", it)
                    }
                    null
                }
            }.toList()
    }

    @OptIn(KspExperimental::class)
    private fun determineSourceParam(function: KSFunctionDeclaration, logger: KSPLogger): KSValueParameter? {
        val parameters = function.parameters
        return when {
            parameters.isEmpty() -> null
            parameters.size > 1 -> {
                val sourceParameter = parameters.filter { it.isAnnotationPresent(Konverter.Source::class) }
                when {
                    sourceParameter.isEmpty() -> null
                    sourceParameter.size > 1 -> {
                        logger.error("Ignored method as multiple parameters were annotated with @Konverter.Source", function)
                        null
                    }

                    else -> sourceParameter.first()
                }
            }

            else -> parameters.first()
        }
    }

    @OptIn(KspExperimental::class)
    private fun determineAdditionalParams(function: KSFunctionDeclaration, sourceParam: KSValueParameter?): List<KSValueParameter> {
        return function.parameters
            .filterNot { it.isAnnotationPresent(Konverter.Source::class) }
            .filterNot { it == sourceParam }
    }

}
