package io.mcarle.lib.kmapper.processor.kmapfrom

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.lib.kmapper.processor.shared.BuilderCache
import io.mcarle.lib.kmapper.processor.shared.CodeBuilder
import io.mcarle.lib.kmapper.processor.shared.QualifiedName
import io.mcarle.lib.kmapper.processor.shared.MapStructureBuilder
import io.mcarle.lib.kmapper.processor.shared.validated

object KMapFromCodeGenerator {

    fun generate(converter: KMapFromConverter, resolver: Resolver, logger: KSPLogger) {
        val mapper = MapStructureBuilder(
            resolver = resolver,
            logger = logger
        )

        val codeBuilder = retrieveCodeBuilder(
            converter.targetClassDeclaration.packageName.asString(),
            converter.targetClassDeclaration.simpleName.asString(),
        )

        codeBuilder.addFunction(
            funSpec = FunSpec.builder(converter.mapFunctionName)
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
                .build(),
            toType = false,
            originating = converter.targetClassDeclaration.containingFile
        )
    }

    private fun retrieveCodeBuilder(
        packageName: String,
        className: String
    ): CodeBuilder {
        val qualifiedName = QualifiedName(packageName, className)
        val codeBuilder = BuilderCache.getOrPut(qualifiedName) {
            CodeBuilder.create(
                qualifiedName, null
            )
        }
        return codeBuilder
    }

}