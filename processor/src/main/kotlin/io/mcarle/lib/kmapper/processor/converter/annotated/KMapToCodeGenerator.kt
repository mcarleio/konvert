package io.mcarle.lib.kmapper.processor.converter.annotated

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.mcarle.lib.kmapper.processor.validated

object KMapToCodeGenerator {

    val builderCache = mutableMapOf<Pair<String, String>, Pair<FileSpec.Builder, List<KSFile>>>()

    fun init() {
        builderCache.clear()
    }

    fun generate(converter: KMapToConverter, resolver: Resolver, logger: KSPLogger) {
        val mapper = MapStructureBuilder(
            resolver = resolver,
            logger = logger
        )

        val fileSpecBuilder = findOrBuildFileBuilder(
            builderCache,
            converter.mapKSClassDeclaration.packageName.asString(),
            converter.mapKSClassDeclaration.simpleName.asString(),
            converter.mapKSClassDeclaration.containingFile
        )

        fileSpecBuilder.addFunction(
            FunSpec.builder(converter.mapFunctionName)
                .returns(converter.targetClassDeclaration.asType(emptyList()).toTypeName())
                .receiver(converter.sourceClassDeclaration.asType(emptyList()).toTypeName())
                .addCode(
                    mapper.rules(
                        converter.annotation.mappings.validated(converter.sourceClassDeclaration, logger),
                        null,
                        converter.sourceClassDeclaration,
                        converter.targetClassDeclaration
                    )
                )
                .build()
        )

    }

    fun write(codeGenerator: CodeGenerator) {
        builderCache.values.forEach { (fileSpecBuilder, originatingKSFiles) ->
            fileSpecBuilder.build().writeTo(
                codeGenerator,
                aggregating = true,
                originatingKSFiles = originatingKSFiles
            )
        }
    }

    private fun findOrBuildFileBuilder(
        cache: MutableMap<Pair<String, String>, Pair<FileSpec.Builder, List<KSFile>>>,
        packageName: String,
        className: String,
        file: KSFile?
    ): FileSpec.Builder {
        val identifier = packageName to "${className}KMapExtensions"
        val pair = cache[identifier]
        if (pair == null) {
            cache[identifier] = (FileSpec.builder(
                packageName = packageName,
                fileName = identifier.second
            )) to listOfNotNull(
                file
            )
        } else {
            cache[identifier] = pair.first to (pair.second + file).filterNotNull().distinct()
        }
        return cache[identifier]!!.first
    }

}