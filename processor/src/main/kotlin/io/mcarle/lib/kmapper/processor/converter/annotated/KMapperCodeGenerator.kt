package io.mcarle.lib.kmapper.processor.converter.annotated

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.mcarle.lib.kmapper.processor.validated

object KMapperCodeGenerator {
    val builderCache = mutableMapOf<Pair<String, String>, Pair<Pair<FileSpec.Builder, TypeSpec.Builder>, List<KSFile>>>()

    fun init() {
        builderCache.clear()
    }

    fun generate(converter: KMapperConverter, resolver: Resolver, logger: KSPLogger) {
        val mapper = MapStructureBuilder(
            resolver = resolver,
            logger = logger
        )

        val typeSpecBuilder = findOrBuildObjectBuilder(
            builderCache,
            converter.mapKSClassDeclaration.packageName.asString(),
            converter.mapKSClassDeclaration.asStarProjectedType(),
            converter.mapKSClassDeclaration.simpleName.asString(),
            converter.mapKSClassDeclaration.containingFile
        )

        typeSpecBuilder.addFunction(
            FunSpec.builder(converter.mapFunctionName)
                .addModifiers(KModifier.OVERRIDE)
                .returns(converter.targetClassDeclaration.asType(emptyList()).toTypeName())
                .addParameter(converter.paramName, converter.sourceClassDeclaration.asType(emptyList()).toTypeName())
                .addCode(
                    mapper.rules(
                        converter.annotation.mappings.asIterable().validated(converter.mapKSFunctionDeclaration, logger),
                        converter.paramName,
                        converter.sourceClassDeclaration,
                        converter.targetClassDeclaration
                    )
                )
                .build()
        )
    }

    fun write(codeGenerator: CodeGenerator) {
        builderCache.values.forEach { (builderPair, originatingKSFiles) ->
            val (fileSpecBuilder, typeSpecBuilder) = builderPair
            fileSpecBuilder.addType(typeSpecBuilder.build())
            fileSpecBuilder.build().writeTo(
                codeGenerator,
                aggregating = false,
                originatingKSFiles = originatingKSFiles
            )
        }

    }

    private fun findOrBuildObjectBuilder(
        cache: MutableMap<Pair<String, String>, Pair<Pair<FileSpec.Builder, TypeSpec.Builder>, List<KSFile>>>,
        packageName: String,
        interfaceType: KSType,
        interfaceName: String,
        file: KSFile?
    ): TypeSpec.Builder {
        val identifier = packageName to "${interfaceName}Impl"
        val pair = cache[identifier]
        if (pair == null) {
            cache[identifier] = (FileSpec.builder(
                packageName = packageName,
                fileName = "${interfaceName}Impl"
            ) to TypeSpec.objectBuilder("${interfaceName}Impl").addSuperinterface(interfaceType.toTypeName())) to listOfNotNull(file)
        } else {
            cache[identifier] = pair.first to (pair.second + file).filterNotNull().distinct()
        }
        return cache[identifier]!!.first.second
    }

}