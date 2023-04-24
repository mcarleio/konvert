package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.konverterGenerateClass
import io.mcarle.konvert.converter.api.config.withIsolatedConfiguration
import io.mcarle.konvert.plugin.api.KonverterInjector
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.validated
import java.util.ServiceLoader

object KonverterCodeGenerator {

    private val injectors by lazy {
        ServiceLoader.load(KonverterInjector::class.java, this::class.java.classLoader).toList()
    }

    fun generate(data: KonverterData, resolver: Resolver, logger: KSPLogger) {
        Configuration.CURRENT += data.annotationData.options.map { it.key to it.value }

        val mapper = CodeGenerator(
            logger = logger
        )

        if (data.konvertData.none { it.annotationData != null }) {
            return
        }

        val codeBuilder = retrieveCodeBuilder(
            data.mapKSClassDeclaration,
            data.mapKSClassDeclaration.packageName.asString(),
            data.mapKSClassDeclaration.asStarProjectedType(),
            data.mapKSClassDeclaration.simpleName.asString()
        )

        data.konvertData.forEach { konvertData ->
            withIsolatedConfiguration {
                if (konvertData.annotationData == null) {
                    return@withIsolatedConfiguration
                }

                Configuration.CURRENT += konvertData.annotationData.options.map { it.key to it.value }

                if (konvertData.sourceTypeReference.toString() != konvertData.sourceType.makeNotNullable().toString()) {
                    // add import alias
                    codeBuilder.addImport(konvertData.sourceType, konvertData.sourceTypeReference.toString())
                }
                val targetClassImportName =
                    if (konvertData.targetTypeReference.toString() != konvertData.targetType.makeNotNullable().toString()) {
                        // add import alias
                        val alias = konvertData.targetTypeReference.toString()
                        codeBuilder.addImport(konvertData.targetType, alias)
                        alias
                    } else if (konvertData.sourceTypeReference.toString() == konvertData.targetTypeReference.toString()) {
                        null
                    } else {
                        konvertData.targetClassDeclaration.simpleName.asString()
                    }

                codeBuilder.addFunction(
                    funSpec = FunSpec.builder(konvertData.mapFunctionName)
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(konvertData.targetTypeReference.toTypeName())
                        .addParameter(konvertData.paramName, konvertData.sourceTypeReference.toTypeName())
                        .addCode(
                            mapper.generateCode(
                                konvertData.annotationData.mappings.asIterable().validated(konvertData.mapKSFunctionDeclaration, logger),
                                konvertData.annotationData.constructor,
                                konvertData.paramName,
                                targetClassImportName,
                                konvertData.sourceType,
                                konvertData.targetType,
                                konvertData.mapKSFunctionDeclaration
                            )
                        )
                        .build(),
                    toType = true,
                    originating = data.mapKSClassDeclaration.containingFile
                )
            }
        }
    }

    private fun retrieveCodeBuilder(
        mapperInterfaceKSClassDeclaration: KSClassDeclaration,
        packageName: String,
        interfaceType: KSType,
        interfaceName: String
    ): CodeBuilder {
        return CodeBuilder.getOrCreate(packageName, interfaceName) {
            if (Configuration.konverterGenerateClass) {
                TypeSpec.classBuilder("${interfaceName}Impl")
            } else {
                TypeSpec.objectBuilder("${interfaceName}Impl")
            }
                .addSuperinterface(interfaceType.toTypeName())
                .also { typeBuilder ->
                    injectors.forEach {
                        it.processType(typeBuilder, mapperInterfaceKSClassDeclaration)
                    }
                }

        }
    }

}
