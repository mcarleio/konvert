package io.mcarle.konvert.processor.konvertfrom

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Visibility
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.mcarle.konvert.converter.api.config.withIsolatedConfiguration
import io.mcarle.konvert.processor.codegen.CodeBuilder
import io.mcarle.konvert.processor.codegen.CodeGenerator
import io.mcarle.konvert.processor.codegen.MappingContext
import io.mcarle.konvert.processor.exceptions.InaccessibleDueToVisibilityClassException
import io.mcarle.konvert.processor.exceptions.KonvertException
import io.mcarle.konvert.processor.isEqualOrMoreRestrictedThan
import io.mcarle.konvert.processor.validated

object KonvertFromCodeGenerator {

    fun generate(
        data: KonvertFromData,
        resolver: Resolver,
        environment: SymbolProcessorEnvironment
    ) = withIsolatedConfiguration(data.annotationData.options) {
        try {
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
                    .addModifiers(
                        *determineModifiers(
                            data.sourceClassDeclaration,
                            data.targetClassDeclaration,
                            data.targetCompanionDeclaration
                        )
                    )
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
        } catch (e: Exception) {
            throw KonvertException(data.sourceClassDeclaration.asStarProjectedType(), data.targetClassDeclaration.asStarProjectedType(), e)
        }
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

    private fun determineModifiers(
        sourceClassDeclaration: KSClassDeclaration,
        targetClassDeclaration: KSClassDeclaration,
        targetCompanionDeclaration: KSClassDeclaration
    ): Array<KModifier> {
        val sourceVisibility = sourceClassDeclaration.getVisibility()
        val targetClassVisibility = targetClassDeclaration.getVisibility()
        val targetCompanionVisibility = targetCompanionDeclaration.getVisibility()

        val (moreRestrictedVisibility, moreRestrictedClassDeclaration) =
            if (targetCompanionVisibility.isEqualOrMoreRestrictedThan(targetClassVisibility)) {
                if (sourceVisibility.isEqualOrMoreRestrictedThan(targetCompanionVisibility)) {
                    sourceVisibility to sourceClassDeclaration
                } else {
                    targetCompanionVisibility to targetCompanionDeclaration
                }
            } else {
                if (sourceVisibility.isEqualOrMoreRestrictedThan(targetClassVisibility)) {
                    sourceVisibility to sourceClassDeclaration
                } else {
                    targetClassVisibility to targetClassDeclaration
                }
            }

        return when (moreRestrictedVisibility) {
            Visibility.PUBLIC -> arrayOf(KModifier.PUBLIC)
            Visibility.JAVA_PACKAGE,
            Visibility.INTERNAL -> arrayOf(KModifier.INTERNAL)
            Visibility.PROTECTED,
            Visibility.LOCAL,
            Visibility.PRIVATE -> throw InaccessibleDueToVisibilityClassException(
                visibility = moreRestrictedVisibility,
                classDeclaration = moreRestrictedClassDeclaration
            )
        }
    }

}
