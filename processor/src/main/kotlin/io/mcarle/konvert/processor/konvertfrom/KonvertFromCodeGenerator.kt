package io.mcarle.konvert.processor.konvertfrom

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.converter.api.config.withIsolatedConfiguration
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.validated

object KonvertFromCodeGenerator {

    fun generate(data: KonvertFromData, resolver: Resolver, logger: KSPLogger) = withIsolatedConfiguration(data.annotationData.options) {

        val mapper = CodeGenerator(
            logger = logger,
            resolver = resolver
        )

        val codeBuilder = CodeBuilder.getOrCreate(
            data.targetClassDeclaration.packageName.asString(),
            data.targetClassDeclaration.simpleName.asString(),
        )

        codeBuilder.addFunction(
            funBuilder = FunSpec.builder(data.mapFunctionName)
                .returns(data.targetClassDeclaration.asStarProjectedType().toTypeName())
                .addParameter(data.paramName, data.sourceClassDeclaration.asStarProjectedType().toTypeName())
                .receiver(data.targetCompanionDeclaration.asStarProjectedType().toTypeName())
                .addCode(
                    mapper.generateCode(
                        data.annotationData.mappings.validated(data.sourceClassDeclaration, logger),
                        data.annotationData.constructor,
                        data.paramName,
                        null,
                        data.sourceClassDeclaration.asStarProjectedType(),
                        data.targetClassDeclaration.asStarProjectedType(),
                        data.targetCompanionDeclaration,
                        emptyList()
                    )
                ),
            priority = data.priority,
            toType = false,
            originating = data.targetClassDeclaration.containingFile
        )
    }

    fun toFunctionFullyQualifiedNames(data: KonvertFromData): List<String> {
        val packageName = data.targetClassDeclaration.packageName.asString()
        return listOf(
            if (packageName.isEmpty()) {
                data.mapFunctionName
            } else {
                "$packageName.${data.mapFunctionName}"
            }
        )
    }

}
