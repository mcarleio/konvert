package io.mcarle.konvert.processor.module

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.parseDeprecatedMetaInfFiles
import io.mcarle.konvert.processor.konvert.KonvertTypeConverter
import io.mcarle.konvert.processor.konvert.KonverterInterface
import io.mcarle.konvert.processor.konvertfrom.KonvertFromTypeConverter
import io.mcarle.konvert.processor.konvertto.KonvertToTypeConverter
import io.mcarle.konvert.processor.typeClassDeclaration

/**
 * Deprecated in favor of the new way Konvert passes FQNs of the
 * generated functions (see [io.mcarle.konvert.processor.module.GeneratedKonvertModuleWriter]).
 *
 * Reads the generated Konverters from the three META-INF files:
 * - `META-INF/konvert/io.mcarle.konvert.api.Konvert`
 * - `META-INF/konvert/io.mcarle.konvert.api.KonvertTo`
 * - `META-INF/konvert/io.mcarle.konvert.api.KonvertFrom`
 */
@Deprecated("Only here to be backwards compatible with older versions of Konvert")
class GeneratedKonverterLoaderFromMetaInf(
    private val resolver: Resolver,
    private val logger: KSPLogger
) {

    fun load(): List<TypeConverter> {
        return if (Configuration.parseDeprecatedMetaInfFiles) {
            loadKonvertTypeConverter() + loadKonvertToTypeConverter() + loadKonvertFromTypeConverter()
        } else {
            emptyList()
        }
    }

    private fun <T : TypeConverter> loadByFunctions(
        resourceFile: String,
        processor: (GeneratedKonverterData) -> T
    ): List<T> {
        return this::class.java.classLoader.getResources(resourceFile)
            .toList()
            .flatMap { it.readText().lineSequence() }
            .filter { it.isNotBlank() }
            .flatMap { GeneratedKonverterData.from(it, resolver, logger) }
            .map(processor)
    }

    private fun <T : TypeConverter> loadByClasses(
        resourceFile: String,
        processor: (GeneratedKonverterData) -> T
    ): List<T> {
        return this::class.java.classLoader.getResources(resourceFile)
            .toList()
            .flatMap { it.readText().lineSequence() }
            .filter { it.isNotBlank() }
            .groupBy { it.substringBeforeLast('.') }
            .flatMap {
                GeneratedKonverterData.from(
                    classFqn = it.key,
                    functionsFqn = it.value,
                    resolver = resolver,
                    logger = logger
                )
            }
            .map(processor)
    }

    private fun loadKonvertTypeConverter(): List<KonvertTypeConverter> {
        return loadByClasses("META-INF/konvert/io.mcarle.konvert.api.${Konvert::class.simpleName}") { data ->
            val closestClassDeclaration = data.function.closestClassDeclaration()
                ?: throw RuntimeException("Could not find class declaration for ${data.function.qualifiedName?.asString()}")
            KonvertTypeConverter(
                priority = data.priority,
                alreadyGenerated = true,
                sourceType = data.function.parameters.first().type.resolve(),
                targetType = data.function.returnType!!.resolve(),
                mapFunctionName = data.function.simpleName.asString(),
                paramName = data.function.parameters.first().name!!.asString(),
                konverterInterface = KonverterInterface(closestClassDeclaration.superTypes.first().resolve().classDeclaration()!!),
                classKind = if (closestClassDeclaration.classKind == ClassKind.CLASS)
                    KonvertTypeConverter.ClassOrObject.CLASS
                else
                    KonvertTypeConverter.ClassOrObject.OBJECT,
            )
        }
    }

    private fun loadKonvertToTypeConverter(): List<KonvertToTypeConverter> {
        return loadByFunctions("META-INF/konvert/io.mcarle.konvert.api.${KonvertTo::class.simpleName}") { data ->
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
        return loadByFunctions("META-INF/konvert/io.mcarle.konvert.api.${KonvertFrom::class.simpleName}") { data ->
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
}
