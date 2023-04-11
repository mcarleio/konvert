package io.mcarle.konvert.processor.konvertfrom

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.validated

object KonvertFromCodeGenerator {

    fun generate(converter: KonvertFromTypeConverter, resolver: Resolver, logger: KSPLogger) {
        val mapper = CodeGenerator(
            logger = logger
        )

        val codeBuilder = CodeBuilder.getOrCreate(
            converter.targetClassDeclaration.packageName.asString(),
            converter.targetClassDeclaration.simpleName.asString(),
        )

        codeBuilder.addFunction(
            funSpec = FunSpec.builder(converter.mapFunctionName)
                .returns(converter.targetClassDeclaration.asStarProjectedType().toTypeName())
                .addParameter(converter.paramName, converter.sourceClassDeclaration.asStarProjectedType().toTypeName())
                .receiver(converter.targetCompanionDeclaration.asStarProjectedType().toTypeName())
                .addCode(
                    mapper.generateCode(
                        converter.annotationData.mappings.validated(converter.sourceClassDeclaration, logger),
                        converter.annotationData.constructor,
                        converter.paramName,
                        converter.targetClassDeclaration.simpleName.asString(),
                        converter.sourceClassDeclaration,
                        converter.targetClassDeclaration,
                        converter.targetCompanionDeclaration
                    )
                )
                .build(),
            toType = false,
            originating = converter.targetClassDeclaration.containingFile
        )
    }

}
