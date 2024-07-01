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

                if (it.isAbstract) {
                    // abstract functions must have a defined source and target type
                    check(source != null && target != null) {
                        "${Konvert::class.simpleName} annotated function must have exactly one source parameter (either single " +
                            "parameter or annotated with @${Konverter::class.simpleName}.${Konverter.Source::class.simpleName}) " +
                            "and must have a return type: ${it.qualifiedName?.asString() ?: it}"
                    }
                }

                if (source != null && target != null) {
                    KonvertData(
                        annotationData = annotation ?: KonvertData.AnnotationData.default(resolver, it.isAbstract),
                        isAbstract = it.isAbstract,
                        isSuspend = Modifier.SUSPEND in it.modifiers,
                        sourceTypeReference = source,
                        targetTypeReference = target,
                        mapKSFunctionDeclaration = it,
                        additionalParameters = determineAdditionalParams(it, sourceValueParameter)
                    )
                } else {
                    if (annotation != null) {
                        logger.warn("Ignoring annotated implemented function as source and/or target could not be determined", it)
                    } else {
                        logger.logging("Ignoring implemented function as source and/or target could not be determined", it)
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
