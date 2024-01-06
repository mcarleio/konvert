package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.api.Konverter
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

    fun generate(data: KonverterData, resolver: Resolver, logger: KSPLogger) = withIsolatedConfiguration(data.annotationData.options) {
        withCurrentKonverterInterface(data.mapKSClassDeclaration) {
            val mapper = CodeGenerator(
                logger = logger
            )

            val codeBuilder = retrieveCodeBuilder(
                data.mapKSClassDeclaration,
                data.mapKSClassDeclaration.packageName.asString(),
                data.mapKSClassDeclaration.asStarProjectedType(),
                data.mapKSClassDeclaration.simpleName.asString()
            )

            data.konvertData.forEach { konvertData ->
                withIsolatedConfiguration(konvertData.annotationData.options) {
                    if (isAlias(konvertData.sourceTypeReference, konvertData.sourceType)) {
                        // @Konverter annotated interface used alias for source, so the implementation should also use the same alias
                        codeBuilder.addImport(konvertData.sourceType, konvertData.sourceTypeReference.toString())
                    }
                    val targetClassImportName =
                        if (isAlias(konvertData.targetTypeReference, konvertData.targetType)) {
                            // @Konverter annotated interface used alias for target, so the implementation should also use the same alias
                            val alias = konvertData.targetTypeReference.toString()
                            codeBuilder.addImport(konvertData.targetType, alias)
                            alias
                        } else if (konvertData.sourceTypeReference.toString() == konvertData.targetTypeReference.toString()) {
                            null
                        } else {
                            konvertData.targetClassDeclaration.simpleName.asString()
                        }

                    codeBuilder.addFunction(
                        funBuilder = FunSpec.builder(konvertData.mapFunctionName)
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(konvertData.targetTypeReference.toTypeName())
                            .addParameters(konvertData.mapKSFunctionDeclaration.parameters.map {
                                val builder = ParameterSpec.builder(
                                    name = it.name!!.asString(),
                                    type = it.type.toTypeName(),
                                    modifiers = emptyArray()
                                )
                                if (it.isVararg) {
                                    builder.addModifiers(KModifier.VARARG)
                                }
                                builder.build()
                            })
                            .apply {
                                if (!konvertData.isAbstract) {
                                    generateSuperCall(konvertData)
                                } else {
                                    generateMappingCode(mapper, konvertData, targetClassImportName, logger)
                                }
                            },
                        priority = konvertData.priority,
                        toType = true,
                        originating = data.mapKSClassDeclaration.containingFile
                    )
                }
            }
        }
    }

    private fun FunSpec.Builder.generateSuperCall(konvertData: KonvertData): FunSpec.Builder {
        return addCode(
            "return super.${konvertData.mapFunctionName}(${konvertData.paramName})"
        )
    }

    private fun FunSpec.Builder.generateMappingCode(
        mapper: CodeGenerator,
        konvertData: KonvertData,
        targetClassImportName: String?,
        logger: KSPLogger
    ): FunSpec.Builder {
        return addCode(
            mapper.generateCode(
                konvertData.annotationData.mappings.asIterable()
                    .validated(konvertData.mapKSFunctionDeclaration, logger),
                konvertData.annotationData.constructor,
                konvertData.paramName,
                targetClassImportName,
                konvertData.sourceType,
                konvertData.targetType,
                konvertData.mapKSFunctionDeclaration,
                konvertData.additionalParameters
            )
        )
    }

    private fun isAlias(typeReference: KSTypeReference, type: KSType): Boolean {
        return typeReference.toString() != type.makeNotNullable().toString().takeWhile { it != '<' }
    }

    private fun retrieveCodeBuilder(
        mapperInterfaceKSClassDeclaration: KSClassDeclaration,
        packageName: String,
        interfaceType: KSType,
        interfaceName: String
    ): CodeBuilder {
        return CodeBuilder.getOrCreate(packageName, interfaceName) {
            if (Configuration.konverterGenerateClass) {
                TypeSpec.classBuilder("${interfaceName}${Konverter.KONVERTER_GENERATED_CLASS_SUFFIX}")
            } else {
                TypeSpec.objectBuilder("${interfaceName}${Konverter.KONVERTER_GENERATED_CLASS_SUFFIX}")
            }
                .addSuperinterface(interfaceType.toTypeName())
                .also { typeBuilder ->
                    injectors.forEach {
                        it.processType(typeBuilder, mapperInterfaceKSClassDeclaration)
                    }
                }

        }
    }

    fun toFunctionFullyQualifiedNames(data: KonverterData): List<String> {
        return data.konvertData
            .filter { it.additionalParameters.isEmpty() } // filter out mappings with more than one parameter
            .map {
                "${data.mapKSClassDeclaration.qualifiedName?.asString()}${Konverter.KONVERTER_GENERATED_CLASS_SUFFIX}.${it.mapFunctionName}"
            }
    }

}
