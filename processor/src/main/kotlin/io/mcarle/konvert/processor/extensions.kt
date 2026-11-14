package io.mcarle.konvert.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Visibility
import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.NoParamDefinedException
import io.mcarle.konvert.api.NotAllowedParameterCombinationException
import io.mcarle.konvert.api.TypeConverterName
import io.mcarle.konvert.api.validate
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.config.Configuration
import io.mcarle.konvert.converter.api.config.InvalidMappingStrategy
import io.mcarle.konvert.converter.api.config.invalidMappingStrategy
import io.mcarle.konvert.processor.exceptions.InvalidMappingException

fun Iterable<Mapping>.validated(reference: KSNode, logger: KSPLogger) = filter { annotation ->
    try {
        annotation.validate()
        true
    } catch (e: NoParamDefinedException) {
        // Filter out, as it is not defined how the target field should be set
        logger.warn(e.message!!, reference)
        false
    } catch (e: NotAllowedParameterCombinationException) {
        when (Configuration.invalidMappingStrategy) {
            InvalidMappingStrategy.WARN -> {
                // Only warn
                logger.warn(e.message!!, reference)
                true
            }
            InvalidMappingStrategy.FAIL -> throw InvalidMappingException.incompatibleParameters(e)
        }
    }
}.also {
    groupBy { it.target }.onEach { (target, mappings) ->
        if (mappings.size > 1) {
            when (Configuration.invalidMappingStrategy) {
                InvalidMappingStrategy.WARN -> logger.warn("Multiple mappings for target=$target", reference)
                InvalidMappingStrategy.FAIL -> throw InvalidMappingException.duplicateTarget(mappings)
            }
        }
    }
}

fun Mapping.Companion.from(annotation: KSAnnotation) = Mapping(
    target = annotation.arguments.first { it.name?.asString() == Mapping::target.name }.value as String,
    source = annotation.arguments.first { it.name?.asString() == Mapping::source.name }.value as String,
    constant = annotation.arguments.first { it.name?.asString() == Mapping::constant.name }.value as String,
    expression = annotation.arguments.first { it.name?.asString() == Mapping::expression.name }.value as String,
    ignore = annotation.arguments.first { it.name?.asString() == Mapping::ignore.name }.value as Boolean,
    enable = (annotation.arguments.first { it.name?.asString() == Mapping::enable.name }.value as List<*>)
        .filterIsInstance<TypeConverterName>()
        .toTypedArray(),
)

fun Konfig.Companion.from(annotation: KSAnnotation) = Konfig(
    key = annotation.arguments.first { it.name?.asString() == Konfig::key.name }.value as String,
    value = annotation.arguments.first { it.name?.asString() == Konfig::value.name }.value as String
)

fun KSValueParameter.typeClassDeclaration(): KSClassDeclaration? = this.type.resolve().classDeclaration()

/**
 * Resolves (possibly nested) type aliases to the type they are aliasing.
 *
 * An alias which does not declare type parameters has no type arguments of its own, even if the aliased
 * type is generic (e.g. `typealias Tags = List<Tag>`). Converters relying on [KSType.arguments] would
 * otherwise fail. The nullability of the alias itself is retained.
 *
 * If the alias itself declares type parameters (e.g. Kotlin's `typealias HashMap<K, V> = java.util.HashMap<K, V>`),
 * the actual type arguments of the alias usage are substituted into the aliased type. Otherwise the arguments
 * of the alias would be replaced by the unresolvable type parameters of the alias declaration.
 */
fun KSType.resolveTypeAliases(): KSType {
    var type = this
    while (true) {
        val alias = type.declaration as? KSTypeAlias ?: return type
        val aliasedType = alias.type.resolve().substituteTypeParameters(alias, type.arguments)
        type = if (type.isMarkedNullable && !aliasedType.isMarkedNullable) aliasedType.makeNullable() else aliasedType
    }
}

/**
 * Replaces the type parameters of the given [alias] inside the already expanded [alias type][this]
 * with the actual type [arguments] of the alias usage.
 */
private fun KSType.substituteTypeParameters(
    alias: KSTypeAlias,
    arguments: List<KSTypeArgument>
): KSType {
    if (arguments.isEmpty()) return this

    val substitution = alias.typeParameters.mapIndexedNotNull { index, parameter ->
        arguments.getOrNull(index)?.let { parameter.name.asString() to it }
    }.toMap()

    return if (substitution.isEmpty()) this else substituteTypeParameters(substitution)
}

private fun KSType.substituteTypeParameters(substitution: Map<String, KSTypeArgument>): KSType {
    val parameter = declaration as? KSTypeParameter
    if (parameter != null) {
        val argumentType = substitution[parameter.name.asString()]?.type?.resolve() ?: return this
        return if (isMarkedNullable && !argumentType.isMarkedNullable) argumentType.makeNullable() else argumentType
    }

    if (arguments.isEmpty()) return this

    var changed = false
    val substitutedArguments = arguments.map { argument ->
        val argumentType = argument.type?.resolve() ?: return@map argument
        val argumentParameter = argumentType.declaration as? KSTypeParameter
        val replacement = argumentParameter?.let { substitution[it.name.asString()] }

        val substitutedArgument = if (replacement != null) {
            val replacementType = replacement.type?.resolve()
            when {
                // A star projection has no type, so keep it as it is.
                replacementType == null -> replacement
                argumentType.isMarkedNullable && !replacementType.isMarkedNullable ->
                    replacement.withType(replacementType.makeNullable())

                else -> replacement
            }
        } else {
            val resolvedType = argumentType.substituteTypeParameters(substitution)
            if (resolvedType === argumentType) argument else argument.withType(resolvedType)
        }

        if (substitutedArgument !== argument) changed = true
        substitutedArgument
    }

    return if (changed) replace(substitutedArguments) else this
}

private fun KSTypeArgument.withType(type: KSType): KSTypeArgument {
    val typeReference = this.type ?: return this
    return DelegatingTypeArgument(this, DelegatingTypeReference(typeReference, type))
}

private class DelegatingTypeArgument(
    private val delegate: KSTypeArgument,
    private val typeReference: KSTypeReference
) : KSTypeArgument by delegate {
    override val type: KSTypeReference get() = typeReference
}

private class DelegatingTypeReference(
    private val delegate: KSTypeReference,
    private val type: KSType
) : KSTypeReference by delegate {
    override fun resolve(): KSType = type
}

fun Visibility.isEqualOrMoreRestrictedThan(other: Visibility): Boolean {
    if (this == other) return true

    return when (this) {
        Visibility.PUBLIC -> false
        Visibility.JAVA_PACKAGE -> other == Visibility.PUBLIC
        Visibility.INTERNAL -> other == Visibility.PUBLIC || other == Visibility.JAVA_PACKAGE
        Visibility.PROTECTED -> other != Visibility.LOCAL && other != Visibility.PRIVATE
        Visibility.LOCAL -> other != Visibility.PRIVATE
        Visibility.PRIVATE -> true
    }
}
