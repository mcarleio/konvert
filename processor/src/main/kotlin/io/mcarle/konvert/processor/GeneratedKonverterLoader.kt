package io.mcarle.konvert.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import io.mcarle.konvert.api.GeneratedKonverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Priority
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.processor.konvert.KonvertTypeConverter
import io.mcarle.konvert.processor.konvertfrom.KonvertFromTypeConverter
import io.mcarle.konvert.processor.konvertto.KonvertToTypeConverter

class GeneratedKonverterLoader(
    private val resolver: Resolver,
    private val logger: KSPLogger
) {

    fun load(): List<TypeConverter> {
        return loadKonvertTypeConverter() + loadKonvertToTypeConverter() + loadKonvertFromTypeConverter()
    }

    private fun <T : TypeConverter> load(
        resourceFile: String,
        processor: (GeneratedKonverterData) -> T
    ): List<T> {
        return ClassLoader.getSystemResources(resourceFile)
            .toList()
            .flatMap { it.readText().lineSequence() }
            .filter { it.isNotBlank() }
            .flatMap { GeneratedKonverterData.from(it, resolver, logger) }
            .map(processor)
    }

    private fun loadKonvertTypeConverter(): List<KonvertTypeConverter> {
        return load("META-INF/konvert/io.mcarle.konvert.api.${Konvert::class.simpleName}") { data ->
            KonvertTypeConverter(
                priority = data.priority,
                alreadyGenerated = true,
                sourceType = data.function.parameters.first().type.resolve(),
                targetType = data.function.returnType!!.resolve(),
                mapFunctionName = data.function.simpleName.asString(),
                paramName = data.function.parameters.first().name!!.asString(),
                mapKSClassDeclaration = data.function.closestClassDeclaration()?.superTypes?.first()?.resolve()
                    ?.classDeclaration()!!
            )
        }
    }

    private fun loadKonvertToTypeConverter(): List<KonvertToTypeConverter> {
        return load("META-INF/konvert/io.mcarle.konvert.api.${KonvertTo::class.simpleName}") { data ->
            KonvertToTypeConverter(
                priority = data.priority,
                alreadyGenerated = true,
                mapFunctionName = data.function.simpleName.asString(),
                sourceClassDeclaration = data.function.extensionReceiver!!.resolve().classDeclaration()!!,
                targetClassDeclaration = data.function.returnType!!.resolve().classDeclaration()!!
            )
        }
    }

    private fun loadKonvertFromTypeConverter(): List<KonvertFromTypeConverter> {
        return load("META-INF/konvert/io.mcarle.konvert.api.${KonvertFrom::class.simpleName}") { data ->
            KonvertFromTypeConverter(
                priority = data.priority,
                alreadyGenerated = true,
                mapFunctionName = data.function.simpleName.asString(),
                paramName = data.function.parameters.first().name!!.asString(),
                sourceClassDeclaration = data.function.parameters.first().typeClassDeclaration()!!,
                targetClassDeclaration = data.function.returnType!!.resolve().classDeclaration()!!
            )
        }
    }

    data class GeneratedKonverterData(
        val function: KSFunctionDeclaration,
        val priority: Priority,
    ) {

        companion object {
            @OptIn(KspExperimental::class)
            fun from(fqn: String, resolver: Resolver, logger: KSPLogger): Sequence<GeneratedKonverterData> =
                resolver.getFunctionDeclarationsByName(fqn, true).mapNotNull {
                    val priority = it.getAnnotationsByType(GeneratedKonverter::class)
                        .firstOrNull()
                        ?.priority

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
    }
}
