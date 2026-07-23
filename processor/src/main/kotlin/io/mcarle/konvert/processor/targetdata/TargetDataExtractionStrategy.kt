package io.mcarle.konvert.processor.targetdata

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock
import io.mcarle.konvert.processor.codegen.MappingVisibilityContext

fun interface TargetDataExtractionStrategy {

    fun extract(
        resolver: Resolver,
        classDeclaration: KSClassDeclaration,
        visibilityContext: MappingVisibilityContext
    ): TargetData

    data class TargetData(
        val classDeclaration: KSClassDeclaration,
        val varProperties: List<TargetVarProperty>,
        val setter: List<TargetSetter>,
        val primaryConstructor: KSFunctionDeclaration?,
        val constructors: List<KSFunctionDeclaration>,
    )

    data class TargetVarProperty(
        val property: KSPropertyDeclaration,
    ) {
        val name = property.simpleName.asString()
    }

    data class TargetSetter(
        val setter: KSFunctionDeclaration,
        val correspondingGetter: KSFunctionDeclaration?
    ) {
        companion object {
            fun extractPropertyName(setter: KSFunctionDeclaration): String {
                return setter.simpleName.asString().removePrefix("set").replaceFirstChar { it.lowercase() }
            }
        }

        val name = extractPropertyName(setter)
        val typeRef = setter.parameters.single().type

        fun generateAssignmentCode(valueToAssign: CodeBlock): CodeBlock {
            return if (correspondingGetter != null) {
                if (correspondingGetter.simpleName.asString().startsWith("is")) {
                    CodeBlock.of("${correspondingGetter.simpleName.asString()}·=·%L", valueToAssign)
                } else {
                    CodeBlock.of("$name·=·%L", valueToAssign)
                }
            } else {
                CodeBlock.of("${setter.simpleName.asString()}(%L)", valueToAssign)
            }
        }
    }

}
