package io.mcarle.konvert.processor.module

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import io.mcarle.konvert.api.GeneratedKonvertModule
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.processor.konvert.KonvertTypeConverter
import io.mcarle.konvert.processor.konvert.KonverterInterface
import io.mcarle.konvert.processor.konvertfrom.KonvertFromTypeConverter
import io.mcarle.konvert.processor.konvertto.KonvertToTypeConverter
import io.mcarle.konvert.processor.typeClassDeclaration

class GeneratedKonvertModuleLoader(
    private val resolver: Resolver,
    private val logger: KSPLogger
) {

    @OptIn(KspExperimental::class)
    fun load(): List<TypeConverter> {
        val generatedModules = resolver
            .getDeclarationsFromPackage(GENERATED_KONVERTER_MODULE_PACKAGE)
            .flatMap { it.getAnnotationsByType(GeneratedKonvertModule::class) }
            .toList()

        return loadKonvertTypeConverter(generatedModules) +
            loadKonvertToTypeConverter(generatedModules) +
            loadKonvertFromTypeConverter(generatedModules)
    }

    private fun loadKonvertTypeConverter(generatedModules: List<GeneratedKonvertModule>): List<KonvertTypeConverter> {
        return generatedModules
            .flatMap { it.konverterFQN.toList() }
            .distinct()
            .groupBy { it.substringBeforeLast('.') }
            .flatMap {
                GeneratedKonverterData.from(
                    classFqn = it.key,
                    functionsFqn = it.value,
                    resolver = resolver,
                    logger = logger
                )
            }
            .map { data ->
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

    private fun loadKonvertToTypeConverter(generatedModules: List<GeneratedKonvertModule>): List<KonvertToTypeConverter> {
        return generatedModules
            .flatMap { it.konvertToFQN.toList() }
            .distinct()
            .flatMap { GeneratedKonverterData.from(it, resolver, logger) }
            .map { data ->
                KonvertToTypeConverter(
                    priority = data.priority,
                    alreadyGenerated = true,
                    mapFunctionName = data.function.simpleName.asString(),
                    sourceClassDeclaration = data.function.extensionReceiver!!.resolve().classDeclaration()!!,
                    targetClassDeclaration = data.function.returnType!!.resolve().classDeclaration()!!
                )
            }
    }

    private fun loadKonvertFromTypeConverter(generatedModules: List<GeneratedKonvertModule>): List<KonvertFromTypeConverter> {
        return generatedModules
            .flatMap { it.konvertFromFQN.toList() }
            .distinct()
            .flatMap { GeneratedKonverterData.from(it, resolver, logger) }
            .map { data ->
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
