package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.validated

object KonvertToCodeGenerator {

    fun generate(converter: KonvertToTypeConverter, resolver: Resolver, logger: KSPLogger) {
        val mapper = CodeGenerator(
            logger = logger
        )

        val fileSpecBuilder = CodeBuilder.getOrCreate(
            converter.sourceClassDeclaration.packageName.asString(),
            converter.sourceClassDeclaration.simpleName.asString(),
        )

        fileSpecBuilder.addFunction(
            funSpec = FunSpec.builder(converter.mapFunctionName)
                .returns(converter.targetClassDeclaration.asType(emptyList()).toTypeName())
                .receiver(converter.sourceClassDeclaration.asType(emptyList()).toTypeName())
                .addCode(
                    mapper.generateCode(
                        converter.annotationData.mappings.validated(converter.sourceClassDeclaration, logger),
                        converter.annotationData.constructor,
                        null,
                        converter.sourceClassDeclaration,
                        converter.targetClassDeclaration,
                        converter.sourceClassDeclaration
                    )
                )
                .build(),
            toType = false,
            originating = converter.sourceClassDeclaration.containingFile
        )

    }

}