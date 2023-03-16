package io.mcarle.lib.kmapper.processor.kmapfrom

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.lib.kmapper.processor.shared.CodeBuilder
import io.mcarle.lib.kmapper.processor.shared.CodeGenerator
import io.mcarle.lib.kmapper.processor.shared.validated

object KMapFromCodeGenerator {

    fun generate(converter: KMapFromConverter, resolver: Resolver, logger: KSPLogger) {
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
                        converter.sourceClassDeclaration,
                        converter.targetClassDeclaration,
                        converter.targetCompanionDeclaration // TODO: check if this is actual right
                    )
                )
                .build(),
            toType = false,
            originating = converter.targetClassDeclaration.containingFile
        )
    }

}