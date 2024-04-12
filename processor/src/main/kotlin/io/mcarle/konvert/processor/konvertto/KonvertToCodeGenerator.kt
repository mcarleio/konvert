package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.converter.api.config.withIsolatedConfiguration
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.validated

object KonvertToCodeGenerator {

    fun generate(data: KonvertToData, resolver: Resolver, logger: KSPLogger) = withIsolatedConfiguration(data.annotationData.options) {

        val mapper = CodeGenerator(
            logger = logger,
            resolver = resolver
        )

        val fileSpecBuilder = CodeBuilder.getOrCreate(
            data.sourceClassDeclaration.packageName.asString(),
            data.sourceClassDeclaration.simpleName.asString(),
        )

        fileSpecBuilder.addFunction(
            funBuilder = FunSpec.builder(data.mapFunctionName)
                .returns(data.targetClassDeclaration.asStarProjectedType().toTypeName())
                .receiver(data.sourceClassDeclaration.asStarProjectedType().toTypeName())
                .addCode(
                    mapper.generateCode(
                        data.annotationData.mappings.validated(data.sourceClassDeclaration, logger),
                        data.annotationData.constructor,
                        null,
                        null,
                        data.sourceClassDeclaration.asStarProjectedType(),
                        data.targetClassDeclaration.asStarProjectedType(),
                        data.sourceClassDeclaration,
                        emptyList()
                    )
                ),
            priority = data.priority,
            toType = false,
            originating = data.sourceClassDeclaration.containingFile
        )

    }

    fun toFunctionFullyQualifiedNames(data: KonvertToData): List<String> {
        val packageName = data.sourceClassDeclaration.packageName.asString()
        return listOf(
            if (packageName.isEmpty()) {
                data.mapFunctionName
            } else {
                "$packageName.${data.mapFunctionName}"
            }
        )
    }

}
