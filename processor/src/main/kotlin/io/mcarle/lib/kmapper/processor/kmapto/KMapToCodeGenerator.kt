package io.mcarle.lib.kmapper.processor.kmapto

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.lib.kmapper.processor.shared.CodeBuilder
import io.mcarle.lib.kmapper.processor.shared.MapStructureBuilder
import io.mcarle.lib.kmapper.processor.shared.validated

object KMapToCodeGenerator {

    fun generate(converter: KMapToConverter, resolver: Resolver, logger: KSPLogger) {
        val mapper = MapStructureBuilder(
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
                        null,
                        converter.sourceClassDeclaration,
                        converter.targetClassDeclaration
                    )
                )
                .build(),
            toType = false,
            originating = converter.sourceClassDeclaration.containingFile
        )

    }

}