package io.mcarle.konvert.processor.sourcedata

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import io.mcarle.konvert.converter.api.isNullable

fun interface SourceDataExtractionStrategy {

    fun extract(
        resolver: Resolver,
        classDeclaration: KSClassDeclaration,
        mappingCodeParentDeclaration: KSDeclaration,
    ): List<SourceData>

    sealed interface SourceData {
        val name: String
        val typeRef: KSTypeReference
        val type: KSType

        companion object {
            fun from(function: KSFunctionDeclaration, resolver: Resolver): SourceData? {
                val functionName = function.simpleName.asString()
                if (matchesGetterName(functionName)) {
                    return SourceGetter(function, resolver)
                }

                if (matchesIsFunction(functionName, function.returnType!!.resolve(), resolver.builtIns.booleanType)) {
                    return SourceFunction(function, resolver)
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
        private val resolver: Resolver
    ) : SourceData {
        override val name: String = property.simpleName.asString()
        override val typeRef: KSTypeReference = property.type
        override val type: KSType = typeRef.resolve().resolveTypeAliases(resolver)
    }

    data class SourceGetter(
        val getter: KSFunctionDeclaration,
        private val resolver: Resolver
    ) : SourceData {
        override val name: String = getter.simpleName.asString()
            .removePrefix("get")
            .replaceFirstChar { it.lowercase() }
        override val typeRef: KSTypeReference = getter.returnType!!
        override val type: KSType = typeRef.resolve().resolveTypeAliases(resolver)
    }

    data class SourceFunction(
        val function: KSFunctionDeclaration,
        private val resolver: Resolver
    ) : SourceData {
        override val name: String = function.simpleName.asString()
        override val typeRef: KSTypeReference = function.returnType!!
        override val type: KSType = typeRef.resolve().resolveTypeAliases(resolver)
    }

}

private fun KSType.resolveTypeAliases(resolver: Resolver): KSType {
    val resolvedType = when (val ksDeclaration = declaration) {
        is KSTypeAlias -> {
            val typeParameterToArgument = ksDeclaration.typeParameters.mapIndexedNotNull { index, typeParameter ->
                arguments.getOrNull(index)?.type?.resolve()?.resolveTypeAliases(resolver)?.let { typeParameter to it }
            }.toMap()

            ksDeclaration.type.resolve()
                .replaceTypeParameters(typeParameterToArgument, resolver)
                .resolveTypeAliases(resolver)
        }

        else -> replaceTypeArguments(resolver)
    }

    return if (isNullable()) {
        resolvedType.makeNullable()
    } else {
        resolvedType
    }
}

private fun KSType.replaceTypeParameters(
    typeParameterToArgument: Map<KSTypeParameter, KSType>,
    resolver: Resolver
): KSType {
    typeParameterToArgument[declaration as? KSTypeParameter]?.let { replacement ->
        return replacement
    }
    return replaceTypeArguments(resolver) {
        it.replaceTypeParameters(typeParameterToArgument, resolver)
    }
}

private fun KSType.replaceTypeArguments(
    resolver: Resolver,
    transformType: (KSType) -> KSType = { it.resolveTypeAliases(resolver) }
): KSType {
    if (arguments.isEmpty()) return this

    val replacedArguments = arguments.map { argument ->
        val argumentType = argument.type?.resolve()?.let(transformType)
        argumentType?.let {
            resolver.getTypeArgument(
                resolver.createKSTypeReferenceFromKSType(it),
                argument.variance
            )
        } ?: argument
    }
    return replace(replacedArguments)
}
