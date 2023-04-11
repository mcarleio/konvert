package io.mcarle.konvert.processor.konvert

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.validated

object KonvertCodeGenerator {

    fun generate(converter: KonvertTypeConverter, resolver: Resolver, logger: KSPLogger) {
        if (converter.annotation == null) {
            return
        }

        val mapper = CodeGenerator(
            logger = logger
        )

        val codeBuilder = retrieveCodeBuilder(
            converter.mapKSClassDeclaration.packageName.asString(),
            converter.mapKSClassDeclaration.asStarProjectedType(),
            converter.mapKSClassDeclaration.simpleName.asString(),
        )

        if (converter.sourceTypeReference.toString() != converter.sourceType.makeNotNullable().toString()) {
            // add import alias
            codeBuilder.addImport(converter.sourceType, converter.sourceTypeReference.toString())
        }
        val targetClassImportName = if (converter.targetTypeReference.toString() != converter.targetType.makeNotNullable().toString()) {
            // add import alias
            val alias = converter.targetTypeReference.toString()
            codeBuilder.addImport(converter.targetType, alias)
            alias
        } else if (converter.sourceTypeReference.toString() == converter.targetTypeReference.toString()) {
            null
        } else {
            converter.targetClassDeclaration.simpleName.asString()
        }

        codeBuilder.addFunction(
            funSpec = FunSpec.builder(converter.mapFunctionName)
                .addModifiers(KModifier.OVERRIDE)
                .returns(converter.targetTypeReference.toTypeName())
                .addParameter(converter.paramName, converter.sourceTypeReference.toTypeName())
                .addCode(
                    mapper.generateCode(
                        converter.annotation.mappings.asIterable().validated(converter.mapKSFunctionDeclaration, logger),
                        converter.annotation.constructor,
                        converter.paramName,
                        targetClassImportName,
                        converter.sourceClassDeclaration,
                        converter.targetClassDeclaration,
                        converter.mapKSFunctionDeclaration
                    )
                )
                .build(),
            toType = true,
            originating = converter.mapKSClassDeclaration.containingFile
        )
    }

    private fun retrieveCodeBuilder(
        packageName: String,
        interfaceType: KSType,
        interfaceName: String,
    ): CodeBuilder {
        return CodeBuilder.getOrCreate(packageName, interfaceName) {
            TypeSpec
                .objectBuilder("${interfaceName}Impl")
                .addSuperinterface(interfaceType.toTypeName())
        }
    }

}
