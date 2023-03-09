package io.mcarle.lib.kmapper.processor.kmapper

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.lib.kmapper.processor.shared.*

object KMapperCodeGenerator {

    fun generate(converter: KMapperConverter, resolver: Resolver, logger: KSPLogger) {
        if (converter.annotation == null) {
            return
        }

        val mapper = MapStructureBuilder(
            resolver = resolver,
            logger = logger
        )

        val codeBuilder = retrieveCodeBuilder(
            converter.mapKSClassDeclaration.packageName.asString(),
            converter.mapKSClassDeclaration.asStarProjectedType(),
            converter.mapKSClassDeclaration.simpleName.asString(),
        )

        codeBuilder.addFunction(
            funSpec = FunSpec.builder(converter.mapFunctionName)
                .addModifiers(KModifier.OVERRIDE)
                .returns(converter.targetClassDeclaration.asType(emptyList()).toTypeName())
                .addParameter(converter.paramName, converter.sourceClassDeclaration.asType(emptyList()).toTypeName())
                .addCode(
                    mapper.rules(
                        converter.annotation.mappings.asIterable().validated(converter.mapKSFunctionDeclaration, logger),
                        converter.paramName,
                        converter.sourceClassDeclaration,
                        converter.targetClassDeclaration
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
        val qualifiedName = QualifiedName(packageName, interfaceName)
        val codeBuilder = BuilderCache.getOrPut(qualifiedName) {
            CodeBuilder.create(
                qualifiedName = qualifiedName,
                typeBuilder = TypeSpec.objectBuilder("${interfaceName}Impl").addSuperinterface(interfaceType.toTypeName()),
            )
        }
        return codeBuilder
    }

}