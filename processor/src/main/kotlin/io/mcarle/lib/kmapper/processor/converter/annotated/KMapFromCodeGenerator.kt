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

object KMapFromCodeGenerator {

    val builderCache = mutableMapOf<Pair<String, String>, Pair<FileSpec.Builder, List<KSFile>>>()

    fun init() {
        builderCache.clear()
    }

    fun generate(converter: KMapFromConverter, resolver: Resolver, logger: KSPLogger) {
        val mapper = MapStructureBuilder(
            resolver = resolver,
            logger = logger
        )

        val fileSpecBuilder = findOrBuildFileBuilder(
            builderCache,
            converter.targetClassDeclaration.packageName.asString(),
            converter.targetClassDeclaration.simpleName.asString(),
            converter.targetClassDeclaration.containingFile
        )

        fileSpecBuilder.addFunction(
            FunSpec.builder(converter.mapFunctionName)
                .returns(converter.targetClassDeclaration.asStarProjectedType().toTypeName())
                .addParameter(converter.paramName, converter.sourceClassDeclaration.asStarProjectedType().toTypeName())
                .receiver(converter.targetCompanionDeclaration.asStarProjectedType().toTypeName())
                .addCode(
                    mapper.rules(
                        converter.annotationData.mappings.validated(converter.sourceClassDeclaration, logger),
                        converter.paramName,
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
        val identifier = packageName to "${className}KMapFromExtensions"
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