package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.validated

object KonvertToCodeGenerator {

    fun generate(data: KonvertToData, resolver: Resolver, logger: KSPLogger) {
        Configuration.CURRENT += data.annotationData.options.map { it.key to it.value }

        val mapper = CodeGenerator(
            logger = logger
        )

        val fileSpecBuilder = CodeBuilder.getOrCreate(
            data.sourceClassDeclaration.packageName.asString(),
            data.sourceClassDeclaration.simpleName.asString(),
        )

        val targetClassImportName =
            if (data.sourceClassDeclaration.simpleName.asString() != data.targetClassDeclaration.simpleName.asString()) {
                data.targetClassDeclaration.simpleName.asString()
            } else {
                null
            }

        fileSpecBuilder.addFunction(
            funSpec = FunSpec.builder(data.mapFunctionName)
                .returns(data.targetClassDeclaration.asStarProjectedType().toTypeName())
                .receiver(data.sourceClassDeclaration.asStarProjectedType().toTypeName())
                .addCode(
                    mapper.generateCode(
                        data.annotationData.mappings.validated(data.sourceClassDeclaration, logger),
                        data.annotationData.constructor,
                        null,
                        targetClassImportName,
                        data.sourceClassDeclaration.asStarProjectedType(),
                        data.targetClassDeclaration.asStarProjectedType(),
                        data.sourceClassDeclaration
                    )
                )
                .build(),
            toType = false,
            originating = data.sourceClassDeclaration.containingFile
        )

    }

}
