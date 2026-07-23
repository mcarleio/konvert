package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.Visibility

sealed interface MappingVisibilityContext {

    fun isVisible(declaration: KSDeclaration): Boolean

    data class Declaration(
        val parentDeclaration: KSDeclaration
    ) : MappingVisibilityContext {
        override fun isVisible(declaration: KSDeclaration): Boolean {
            return declaration.isVisibleFrom(parentDeclaration)
        }
    }

    data class TopLevelInPackage(
        val packageName: String
    ) : MappingVisibilityContext {
        override fun isVisible(declaration: KSDeclaration): Boolean {
            return when (declaration.getVisibility()) {
                Visibility.PUBLIC,
                Visibility.INTERNAL -> true
                Visibility.JAVA_PACKAGE -> declaration.packageName.asString() == packageName
                Visibility.PROTECTED,
                Visibility.LOCAL,
                Visibility.PRIVATE -> false
            }
        }
    }
}

fun KSDeclaration.isVisibleFrom(visibilityContext: MappingVisibilityContext): Boolean {
    return visibilityContext.isVisible(this)
}

