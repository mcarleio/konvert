package io.mcarle.konvert.processor.konvertto

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.converter.api.config.withIsolatedConfiguration
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.codegen.MappingContext
import io.mcarle.konvert.processor.exceptions.KonvertException
import io.mcarle.konvert.processor.exceptions.UnaccessibleDueToVisibilityClassException
import io.mcarle.konvert.processor.isMorePrivateThan
import io.mcarle.konvert.processor.validated

object KonvertToCodeGenerator {

    fun generate(data: KonvertToData, resolver: Resolver, logger: KSPLogger) = withIsolatedConfiguration(data.annotationData.options) {
        try {
            val mapper = CodeGenerator(
                logger = logger,
                resolver = resolver
            )

            val fileSpecBuilder = CodeBuilder.getOrCreate(
                data.sourceClassDeclaration.packageName.asString(),
                data.sourceClassDeclaration.simpleName.asString(),
            )

            fileSpecBuilder.addFunction(
                funBuilder = FunSpec.builder(data.mapFunctionName)
                    .addModifiers(*determineModifiers(data.sourceClassDeclaration, data.targetClassDeclaration))
                    .returns(data.targetClassDeclaration.asStarProjectedType().toTypeName())
                    .receiver(data.sourceClassDeclaration.asStarProjectedType().toTypeName())
                    .addCode(
                        mapper.generateCode(
                            mappings = data.annotationData.mappings.validated(data.sourceClassDeclaration, logger),
                            enforcedConstructorTypes = data.annotationData.constructor,
                            context = MappingContext(
                                sourceClassDeclaration = data.sourceClassDeclaration,
                                targetClassDeclaration = data.targetClassDeclaration,
                                source = data.sourceClassDeclaration.asStarProjectedType(),
                                target = data.targetClassDeclaration.asStarProjectedType(),
                                paramName = null,
                                targetClassImportName = null
                            ),
                            mappingCodeParentDeclaration = data.sourceClassDeclaration,
                            additionalSourceParameters = emptyList()
                        )
                    ),
                priority = data.priority,
                toType = false,
                originating = data.sourceClassDeclaration.containingFile
            )
        } catch (e: Exception) {
            throw KonvertException(data.sourceClassDeclaration.asStarProjectedType(), data.targetClassDeclaration.asStarProjectedType(), e)
        }
    }

    fun toFunctionFullyQualifiedNames(data: KonvertToData): List<String> {
        val packageName = data.sourceClassDeclaration.packageName.asString()
        return listOf(
            if (packageName.isEmpty()) {
                data.mapFunctionName
            } else {
                "$packageName.${data.mapFunctionName}"
            }
        )
    }

    private fun determineModifiers(
        sourceClassDeclaration: KSClassDeclaration,
        targetClassDeclaration: KSClassDeclaration
    ): Array<KModifier> {
        val sourceVisibility = sourceClassDeclaration.getVisibility()
        val targetVisibility = targetClassDeclaration.getVisibility()

        val morePrivateClassDeclaration = if (sourceVisibility.isMorePrivateThan(targetVisibility)) {
            sourceClassDeclaration
        } else {
            targetClassDeclaration
        }

        val visibility = if (morePrivateClassDeclaration === sourceClassDeclaration) {
            sourceVisibility
        } else {
            targetVisibility
        }

        return when (visibility) {
            Visibility.PUBLIC -> arrayOf(KModifier.PUBLIC)
            Visibility.JAVA_PACKAGE,
            Visibility.INTERNAL -> arrayOf(KModifier.INTERNAL)
            Visibility.PROTECTED,
            Visibility.LOCAL,
            Visibility.PRIVATE -> throw UnaccessibleDueToVisibilityClassException(
                visibility = visibility,
                classDeclaration = morePrivateClassDeclaration
            )
        }
    }

}
