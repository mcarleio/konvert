package io.mcarle.konvert.processor.sourcedata

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Origin
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.ResolvedType
import io.mcarle.konvert.processor.TypeSubstitution
import io.mcarle.konvert.processor.codegen.MappingVisibilityContext
import io.mcarle.konvert.processor.resolveFully

fun interface SourceDataExtractionStrategy {

    fun extract(
        resolver: Resolver,
        sourceType: ResolvedType,
        classDeclaration: KSClassDeclaration,
        visibilityContext: MappingVisibilityContext,
    ): List<SourceData>

    sealed interface SourceData {
        val name: String
        val type: ResolvedType
        val accessCode: String

        companion object {
            fun from(
                function: KSFunctionDeclaration,
                substitution: TypeSubstitution,
                resolver: Resolver
            ): SourceData? {
                val functionName = function.simpleName.asString()
                if (matchesGetterName(functionName)) {
                    return SourceGetter(function, substitution, resolver)
                }

                if (matchesIsFunction(functionName, function.returnType!!.resolve(), resolver.builtIns.booleanType)) {
                    return SourceFunction(function, substitution, resolver)
                }

                return null
            }

            private fun matchesGetterName(functionName: String) = functionName.startsWith("get") && !functionName[3].isLowerCase()
            private fun matchesIsFunction(functionName: String, returnType: KSType, booleanType: KSType): Boolean {
                return functionName.startsWith("is")
                    && !functionName[2].isLowerCase()
                    && !returnType.isNullable() // CHECKME: Is this (still) required?
                    && returnType == booleanType
            }
        }
    }

    data class SourceProperty(
        private val property: KSPropertyDeclaration,
        private val substitution: TypeSubstitution,
        private val resolver: Resolver
    ) : SourceData {
        override val name: String = property.simpleName.asString()
        override val type: ResolvedType by lazy { property.type.resolveFully(resolver, substitution) }
        override val accessCode: String = name
    }

    data class SourceGetter(
        private val getter: KSFunctionDeclaration,
        private val substitution: TypeSubstitution,
        private val resolver: Resolver
    ) : SourceData {
        override val name: String = getter.simpleName.asString()
            .removePrefix("get")
            .replaceFirstChar { it.lowercase() }
        override val type: ResolvedType by lazy { getter.returnType!!.resolveFully(resolver, substitution) }
        override val accessCode: String =
            if (getter.origin in listOf(Origin.JAVA, Origin.JAVA_LIB)) name else "${getter.simpleName.asString()}()"
    }

    /**
     * Only used for isXyz-Functions and java records
     */
    data class SourceFunction(
        private val function: KSFunctionDeclaration,
        private val substitution: TypeSubstitution,
        private val resolver: Resolver
    ) : SourceData {
        override val name: String = function.simpleName.asString()
        override val type: ResolvedType by lazy { function.returnType!!.resolveFully(resolver, substitution) }

        /**
         * no need for `()` suffix
         */
        override val accessCode: String = name
    }

}
