package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
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
import io.mcarle.konvert.processor.ResolvedType
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.codegen.MappingContext
import io.mcarle.konvert.processor.codegen.MappingVisibilityContext
import io.mcarle.konvert.processor.exceptions.KonvertException
import io.mcarle.konvert.processor.validated
import java.util.ServiceLoader

object KonverterCodeGenerator {

    private val injectors by lazy {
        ServiceLoader.load(KonverterInjector::class.java, this::class.java.classLoader).toList()
    }

    fun generate(
        data: KonverterData,
        resolver: Resolver,
        environment: SymbolProcessorEnvironment
    ) = withIsolatedConfiguration(data.annotationData.options) {
        withCurrentKonverterInterface(data.konverterInterface) {
            val mapper = CodeGenerator(
                logger = environment.logger,
                resolver = resolver
            )

            val codeBuilder = retrieveCodeBuilder(
                data.konverterInterface
            )

            data.konvertData.forEach { konvertData ->
                withIsolatedConfiguration(konvertData.annotationData.options) {
                    try {
                        val returnTypeReference = checkNotNull(konvertData.mapKSFunctionDeclaration.returnType) {
                            "Missing return type on ${konvertData.mapKSFunctionDeclaration.qualifiedName?.asString()}"
                        }

                        // @Konverter annotated interface used an import alias for source, so the implementation should also use the same alias
                        codeBuilder.addAliasedImportIfNeeded(konvertData.sourceParameter.type, konvertData.sourceType)

                        // @Konverter annotated interface used an import alias for target, so the implementation should also use the same
                        // alias, additionally also to instantiate the target class
                        val targetClassImportName = codeBuilder.addAliasedImportIfNeeded(returnTypeReference, konvertData.targetType)

                        codeBuilder.addFunction(
                            funBuilder = FunSpec.builder(konvertData.mapFunctionName)
                                .addModifiers(KModifier.OVERRIDE)
                                .returns(returnTypeReference.toTypeName())
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
                                    if (konvertData.isSuspend) {
                                        addModifiers(KModifier.SUSPEND)
                                    }
                                    if (!konvertData.isAbstract) {
                                        generateSuperCall(konvertData)
                                    } else {
                                        generateMappingCode(mapper, konvertData, targetClassImportName, environment.logger)
                                    }
                                },
                            priority = konvertData.priority,
                            toType = true,
                            originating = data.konverterInterface.kSClassDeclaration.containingFile
                        )
                    } catch (e: Exception) {
                        throw KonvertException(konvertData.sourceType.ksType, konvertData.targetType.ksType, e)
                    }
                }
            }
        }
    }

    private fun FunSpec.Builder.generateSuperCall(konvertData: KonvertData): FunSpec.Builder {
        return if (konvertData.additionalParameters.isEmpty()) {
            addCode(
                "return·super.${konvertData.mapFunctionName}(${konvertData.paramName})"
            )
        } else {
            addCode(
                "return·super.${konvertData.mapFunctionName}(«\n${konvertData.paramName}·=·${konvertData.paramName},\n"
                    + konvertData.additionalParameters.joinToString(separator = ",\n") {
                    val paramName = it.name?.asString()!!
                    "$paramName·=·$paramName"
                } + "»\n)"
            )
        }
    }

    private fun FunSpec.Builder.generateMappingCode(
        mapper: CodeGenerator,
        konvertData: KonvertData,
        targetClassImportName: String?,
        logger: KSPLogger
    ): FunSpec.Builder {
        return addCode(
            mapper.generateCode(
                mappings = konvertData.annotationData.mappings.asIterable()
                    .validated(konvertData.mapKSFunctionDeclaration, logger),
                enforcedConstructorTypes = konvertData.annotationData.constructor,
                context = MappingContext(
                    sourceClassDeclaration = konvertData.sourceClassDeclaration,
                    targetClassDeclaration = konvertData.targetClassDeclaration,
                    source = konvertData.sourceType,
                    target = konvertData.targetType,
                    paramName = konvertData.paramName,
                    targetClassImportName = targetClassImportName,
                ),
                visibilityContext = MappingVisibilityContext.Declaration(konvertData.mapKSFunctionDeclaration),
                additionalSourceParameters = konvertData.additionalParameters
            )
        )
    }

    private fun isImportAlias(typeReference: KSTypeReference, type: KSType): Boolean {
        // Waiting for solution of https://github.com/google/ksp/issues/2391
        // to be able to identify import alias. The following is a workaround only working on return types.
        return typeReference.toString().takeWhile { it != '<' }.removeSuffix("?") != type.makeNotNullable().toString().takeWhile { it != '<' }
    }

    /**
     * In case the given [typeReference] is written using an import alias, the same alias is added as import to the
     * generated file and returned. Otherwise, `null` is returned.
     *
     * Note: This currently only works for the return type, as KSP provides a PSI backed KSTypeReference for it.
     *       The type of a KSValueParameter is always a resolved one (KSTypeReferenceResolvedImpl), which does not know
     *       anything about the import alias used in the source code.
     *
     * @param typeReference the reference as it was written in the `@Konverter` annotated interface
     * @param type the fully resolved type of the [typeReference], which is the one to be imported
     */
    private fun CodeBuilder.addAliasedImportIfNeeded(typeReference: KSTypeReference, type: ResolvedType): String? {
        if (!isImportAlias(typeReference, typeReference.resolve())) return null

        val alias = typeReference.toString()
        addImport(type.ksType, alias)
        return alias
    }

    private fun retrieveCodeBuilder(
        konverterInterface: KonverterInterface
    ): CodeBuilder {
        return CodeBuilder.getOrCreate(konverterInterface.packageName, konverterInterface.simpleName) {
            if (Configuration.konverterGenerateClass) {
                TypeSpec.classBuilder("${konverterInterface.simpleName}${Konverter.KONVERTER_GENERATED_CLASS_SUFFIX}")
            } else {
                TypeSpec.objectBuilder("${konverterInterface.simpleName}${Konverter.KONVERTER_GENERATED_CLASS_SUFFIX}")
            }
                .addSuperinterface(konverterInterface.typeName)
                .also { typeBuilder ->
                    injectors.forEach {
                        it.processType(typeBuilder, konverterInterface.kSClassDeclaration)
                    }
                    if (konverterInterface.isInternal) {
                        typeBuilder.addModifiers(KModifier.INTERNAL)
                    }
                }

        }
    }

    fun toFunctionFullyQualifiedNames(data: KonverterData): List<String> {
        return data.konvertData
            .filter { it.additionalParameters.isEmpty() } // filter out mappings with more than one parameter
            .map {
                val packageName = data.konverterInterface.packageName
                val simpleName = data.konverterInterface.simpleName + Konverter.KONVERTER_GENERATED_CLASS_SUFFIX
                val functionName = it.mapFunctionName

                if (packageName.isEmpty()) {
                    "$simpleName.$functionName"
                } else {
                    "$packageName.$simpleName.$functionName"
                }
            }
    }

}
