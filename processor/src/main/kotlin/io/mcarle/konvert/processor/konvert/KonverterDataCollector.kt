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

                if (it.extensionReceiver != null) {
                    // ignore extension functions
                    return@mapNotNull null
                }

                val targetValueParameters = determineTargetParams(it)
                if (targetValueParameters.size > 1) {
                    logger.error(
                        "Ignored function as multiple parameters were annotated with " +
                            "@${Konverter::class.simpleName}.${Konverter.Target::class.simpleName}",
                        it
                    )
                    return@mapNotNull null
                }
                val targetValueParameter = targetValueParameters.firstOrNull()
                val sourceValueParameter = determineSourceParam(it, targetValueParameter, logger)
                val source = sourceValueParameter?.type
                val returnedTarget = it.returnType?.let { returnType ->
                    if (returnType.resolve().declaration == resolver.getClassDeclarationByName<Unit>()) {
                        null
                    } else {
                        returnType
                    }
                }
                // a @Konverter.Target annotated parameter defines the target instance to map into, so the function
                // does not need to return the target
                val target = targetValueParameter?.type ?: returnedTarget

                if (targetValueParameter != null && returnedTarget != null) {
                    // the only sensible thing such a function can return is the updated target instance itself
                    check(returnedTarget.resolve() == targetValueParameter.type.resolve()) {
                        "${Konvert::class.simpleName} annotated function must return the type of its " +
                            "@${Konverter::class.simpleName}.${Konverter.Target::class.simpleName} annotated parameter " +
                            "or nothing at all: ${it.qualifiedName?.asString() ?: it}"
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
                            "and must either have a return type or a parameter annotated with " +
                            "@${Konverter::class.simpleName}.${Konverter.Target::class.simpleName}: " +
                            "${it.qualifiedName?.asString() ?: it}"
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
                        additionalParameters = determineAdditionalParams(it, sourceValueParameter, targetValueParameter),
                        targetParameter = targetValueParameter,
                        returnsTargetParameter = targetValueParameter != null && returnedTarget != null
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
    private fun determineTargetParams(function: KSFunctionDeclaration): List<KSValueParameter> {
        return function.parameters.filter { it.isAnnotationPresent(Konverter.Target::class) }
    }

    @OptIn(KspExperimental::class)
    private fun determineSourceParam(
        function: KSFunctionDeclaration,
        targetParam: KSValueParameter?,
        logger: KSPLogger
    ): KSValueParameter? {
        val parameters = function.parameters - listOfNotNull(targetParam)
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
    private fun determineAdditionalParams(
        function: KSFunctionDeclaration,
        sourceParam: KSValueParameter?,
        targetParam: KSValueParameter?
    ): List<KSValueParameter> {
        return function.parameters
            .filterNot { it.isAnnotationPresent(Konverter.Source::class) }
            .filterNot { it.isAnnotationPresent(Konverter.Target::class) }
            .filterNot { it == sourceParam }
            .filterNot { it == targetParam }
    }

}
