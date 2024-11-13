package io.mcarle.konvert.processor.module

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.mcarle.konvert.api.GeneratedKonverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Priority

data class GeneratedKonverterData(
    val function: KSFunctionDeclaration,
    val priority: Priority,
) {

    companion object {
        fun from(
            classFqn: String,
            functionsFqn: List<String>,
            resolver: Resolver,
            logger: KSPLogger
        ): Sequence<GeneratedKonverterData> {
            return resolver.getClassDeclarationByName(classFqn)?.let { classDecl ->
                classDecl.getAllFunctions()
                    .filter { funDecl -> funDecl.qualifiedName?.asString() in functionsFqn }
                    .mapNotNull {
                        val priority = extractPriority(it)

                        if (priority != null) {
                            GeneratedKonverterData(
                                function = it,
                                priority = priority
                            )
                        } else {
                            logger.logging("Ignoring $classFqn, as there is no ${GeneratedKonverter::class.simpleName}, ${Konvert::class.simpleName} or ${Konverter::class.simpleName} annotation")
                            null
                        }
                    }
            } ?: emptySequence()
        }

        fun from(
            fqn: String,
            resolver: Resolver,
            logger: KSPLogger
        ): Sequence<GeneratedKonverterData> {
            return resolver.getFunctionDeclarationsByName(fqn, true).mapNotNull {
                val priority = extractPriority(it)

                if (priority != null) {
                    GeneratedKonverterData(
                        function = it,
                        priority = priority
                    )
                } else {
                    logger.logging("Ignoring $fqn, as there is no ${GeneratedKonverter::class.simpleName} annotation")
                    null
                }
            }
        }

        @OptIn(KspExperimental::class)
        private fun extractPriority(funDeclaration: KSFunctionDeclaration): Priority? {
            return funDeclaration.getAnnotationsByType(GeneratedKonverter::class)
                .firstOrNull()
                ?.priority
        }

    }
}
