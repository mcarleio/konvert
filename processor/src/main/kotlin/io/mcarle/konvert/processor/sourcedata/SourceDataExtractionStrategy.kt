package io.mcarle.konvert.processor.sourcedata

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Origin
import io.mcarle.konvert.converter.api.isNullable
import io.mcarle.konvert.processor.codegen.MappingVisibilityContext

fun interface SourceDataExtractionStrategy {

    fun extract(
        resolver: Resolver,
        classDeclaration: KSClassDeclaration,
        visibilityContext: MappingVisibilityContext,
    ): List<SourceData>

    sealed interface SourceData {
        val name: String
        val typeRef: KSTypeReference
        val accessCode: String

        companion object {
            fun from(function: KSFunctionDeclaration, resolver: Resolver): SourceData? {
                val functionName = function.simpleName.asString()
                if (matchesGetterName(functionName)) {
                    return SourceGetter(function)
                }

                if (matchesIsFunction(functionName, function.returnType!!.resolve(), resolver.builtIns.booleanType)) {
                    return SourceFunction(function)
                }

                return null
            }

            private fun matchesGetterName(functionName: String) = functionName.startsWith("get") && !functionName[3].isLowerCase()
            private fun matchesIsFunction(functionName: String, returnType: KSType, booleanType: KSType): Boolean {
                return functionName.startsWith("is")
                    && !functionName[2].isLowerCase()
                    && !returnType.isNullable()
                    && returnType == booleanType
            }
        }
    }

    data class SourceProperty(
        val property: KSPropertyDeclaration,
    ) : SourceData {
        override val name: String = property.simpleName.asString()
        override val typeRef: KSTypeReference = property.type
        override val accessCode: String = name
    }

    data class SourceGetter(
        val getter: KSFunctionDeclaration
    ) : SourceData {
        override val name: String = getter.simpleName.asString()
            .removePrefix("get")
            .replaceFirstChar { it.lowercase() }
        override val typeRef: KSTypeReference = getter.returnType!!
        override val accessCode: String =
            if (getter.origin in listOf(Origin.JAVA, Origin.JAVA_LIB)) name else "${getter.simpleName.asString()}()"
    }

    /**
     * Only used for isXyz-Functions and java records
     */
    data class SourceFunction(
        val function: KSFunctionDeclaration
    ) : SourceData {
        override val name: String = function.simpleName.asString()
        override val typeRef: KSTypeReference = function.returnType!!

        /**
         * no need for `()` suffix
         */
        override val accessCode: String = name
    }

}
