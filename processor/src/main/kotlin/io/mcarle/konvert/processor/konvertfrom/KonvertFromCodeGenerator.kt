package io.mcarle.konvert.processor.konvertfrom

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.converter.api.config.withIsolatedConfiguration
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.codegen.MappingContext
import io.mcarle.konvert.processor.validated

object KonvertFromCodeGenerator {

    fun generate(
        data: KonvertFromData,
        resolver: Resolver,
        environment: SymbolProcessorEnvironment
    ) = withIsolatedConfiguration(data.annotationData.options) {

        val mapper = CodeGenerator(
            logger = environment.logger,
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
                        mappings = data.annotationData.mappings.validated(data.sourceClassDeclaration, environment.logger),
                        enforcedConstructorTypes = data.annotationData.constructor,
                        context = MappingContext(
                            sourceClassDeclaration = data.sourceClassDeclaration,
                            targetClassDeclaration = data.targetClassDeclaration,
                            source = data.sourceClassDeclaration.asStarProjectedType(),
                            target = data.targetClassDeclaration.asStarProjectedType(),
                            paramName = data.paramName,
                            targetClassImportName = null,
                        ),
                        mappingCodeParentDeclaration = data.targetCompanionDeclaration,
                        additionalSourceParameters = emptyList()
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
