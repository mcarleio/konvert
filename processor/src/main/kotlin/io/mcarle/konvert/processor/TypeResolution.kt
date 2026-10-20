package io.mcarle.konvert.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Variance
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.converter.api.isNullable

/**
 * A [KSType] which is guaranteed to be fully resolved, which means:
 * * all typealiases (also nested ones inside type arguments) are expanded to their underlying type
 * * all type parameters are replaced by their matching type arguments (as far as they are known)
 *
 * The constructor is private and the only way to obtain an instance is [resolveFully]. Therefore, it is
 * impossible to accidentally pass an unresolved type where a resolved one is required.
 *
 * Use [ksType] to pass the type to APIs working on plain [KSType] (e.g. `TypeConverter`).
 */
@JvmInline
value class ResolvedType private constructor(val ksType: KSType) {

    val declaration: KSDeclaration get() = ksType.declaration

    fun classDeclaration(): KSClassDeclaration? = ksType.classDeclaration()

    fun isNullable(): Boolean = ksType.isNullable()

    override fun toString(): String = ksType.toString()

    companion object {
        internal fun of(type: KSType, resolver: Resolver, substitution: TypeSubstitution): ResolvedType =
            ResolvedType(type.fullyResolved(resolver, substitution))
    }

}

/**
 * @see ResolvedType
 */
fun KSTypeReference.resolveFully(resolver: Resolver, substitution: TypeSubstitution = TypeSubstitution.EMPTY): ResolvedType =
    this.resolve().resolveFully(resolver, substitution)

/**
 * @see ResolvedType
 */
fun KSType.resolveFully(resolver: Resolver, substitution: TypeSubstitution = TypeSubstitution.EMPTY): ResolvedType =
    ResolvedType.of(this, resolver, substitution)

/**
 * Recursively expands all typealiases and replaces all type parameters known by the given [substitution].
 *
 * Examples (with `typealias Tags<T> = List<Tag<T>>`):
 * * `Tags<Int>` -> `List<Tag<Int>>`
 * * `Tags<Int>?` -> `List<Tag<Int>>?`
 * * `Map<String, Tags<Int>>` -> `Map<String, List<Tag<Int>>>`
 * * `Tags<*>` -> `List<Tag<*>>`
 *
 * Type parameters, for which no type argument is available, are kept as they are.
 * Type parameters bound to a star projection are replaced by their effective upper bound (`Any?` if unbounded).
 * Inside a type argument the star projection is kept instead, respectively narrowed to `out Bound` if the type
 * parameter declares an upper bound (e.g. `Box<*>.values` of `class Box<T : Number>(val values: List<T>)`
 * resolves to `List<out Number>`).
 *
 * The returned [KSType] is always a "real" type created by the [resolver] (and not a wrapper/delegate),
 * so that all KSP functionality (like `isAssignableFrom`, `arguments`, ...) keeps working as expected.
 */
private fun KSType.fullyResolved(resolver: Resolver, substitution: TypeSubstitution): KSType {
    if (this.isError) return this

    return when (val declaration = this.declaration) {
        is KSTypeParameter -> (substitution.typeOf(declaration)
            ?: declaration.effectiveUpperBoundIfStar(resolver, substitution))
            ?.let { if (this.isMarkedNullable) it.makeNullable() else it }
            ?: this

        is KSTypeAlias -> declaration.type.resolve()
            .fullyResolved(resolver, TypeSubstitution.of(declaration, this.arguments, resolver, substitution))
            .let { if (this.isMarkedNullable) it.makeNullable() else it }

        else -> this.withFullyResolvedArguments(resolver, substitution)
    }
}

/**
 * A type parameter bound to a star projection (e.g. the `T` of `class Box<T>` when resolving `Box<*>`) has no
 * type argument at all. As a star projection cannot be used outside of a type argument position (e.g. as the
 * type of the property `Box<T>.value`), the type parameter is approximated by its effective upper bound
 * (`Any?` if unbounded) - exactly like Kotlin does when accessing such a member through a star projection.
 */
private fun KSTypeParameter.effectiveUpperBoundIfStar(resolver: Resolver, substitution: TypeSubstitution): KSType? {
    if (!substitution.isStar(this)) return null

    return this.resolvedUpperBound(resolver, substitution) ?: resolver.builtIns.anyType.makeNullable()
}

/**
 * The single declared upper bound (already fully resolved) or `null`, if the type parameter is unbounded,
 * declares multiple bounds (which would require an intersection type, that cannot be represented) or if its
 * bound is currently being resolved already (e.g. the self-referencing `T` of `class Box<T : List<T>>`).
 */
private fun KSTypeParameter.resolvedUpperBound(resolver: Resolver, substitution: TypeSubstitution): KSType? {
    if (substitution.isResolvingBoundOf(this)) return null

    return this.bounds.toList().singleOrNull()
        ?.resolve()
        ?.fullyResolved(resolver, substitution.resolvingBoundOf(this))
}

private fun KSType.withFullyResolvedArguments(resolver: Resolver, substitution: TypeSubstitution): KSType {
    if (this.arguments.isEmpty()) return this

    val typeParameters = this.declaration.typeParameters
    var changed = false
    val resolvedArguments = this.arguments.mapIndexed { index, argument ->
        val parameterVariance = typeParameters.getOrNull(index)?.variance ?: Variance.INVARIANT
        argument.fullyResolved(resolver, substitution, parameterVariance).also { if (it !== argument) changed = true }
    }

    return if (changed) this.replace(resolvedArguments) else this
}

private fun KSTypeArgument.fullyResolved(
    resolver: Resolver,
    substitution: TypeSubstitution,
    parameterVariance: Variance
): KSTypeArgument {
    if (this.variance == Variance.STAR) return this
    val typeReference = this.type ?: return this

    val type = typeReference.resolve()

    val declaration = type.declaration
    if (declaration is KSTypeParameter && substitution.isStar(declaration)) {
        return declaration.starProjectionAt(resolver, substitution, typeReference, this.variance, parameterVariance)
    }

    val resolvedType = type.fullyResolved(resolver, substitution)

    return if (resolvedType === type) {
        this
    } else {
        resolver.getTypeArgument(resolver.createKSTypeReferenceFromKSType(resolvedType), this.variance)
    }
}

/**
 * Recreates the star projection of a type parameter which is bound to one (e.g. `Tags<*>` with
 * `typealias Tags<T> = List<T>` -> `List<*>`).
 *
 * If the type parameter declares an upper bound, the more precise `out Bound` projection is used, e.g. the
 * property `values` of `class Box<T : Number>(val values: List<T>)` is a `List<out Number>` when being read
 * through a `Box<*>` - exactly like Kotlin approximates the captured type of such a member.
 * In an `in` position that approximation is not possible (there a star projection is equivalent to
 * `in Nothing`), therefore the star projection is kept.
 */
private fun KSTypeParameter.starProjectionAt(
    resolver: Resolver,
    substitution: TypeSubstitution,
    typeReference: KSTypeReference,
    useSiteVariance: Variance,
    parameterVariance: Variance
): KSTypeArgument {
    val starProjection = resolver.getTypeArgument(typeReference, Variance.STAR)

    if (useSiteVariance == Variance.CONTRAVARIANT || parameterVariance == Variance.CONTRAVARIANT) return starProjection

    // unbounded (or multiple bounds) would result in `out Any?`, which is equivalent to the star projection
    val upperBound = this.resolvedUpperBound(resolver, substitution) ?: return starProjection

    return resolver.getTypeArgument(resolver.createKSTypeReferenceFromKSType(upperBound), Variance.COVARIANT)
}

/**
 * Holds the mapping of type parameters (e.g. the `T` of `class Tag<T>` or of `typealias Tags<T> = List<T>`)
 * to their actual, already fully resolved (see [ResolvedType]) type arguments.
 */
class TypeSubstitution private constructor(
    private val substitutions: Map<String, Substitute>,
    /**
     * The type parameters whose upper bound is currently being resolved, which is required to stop the
     * resolution of self-referencing bounds (e.g. the `T` of `class Box<T : List<T>>`).
     */
    private val boundsInResolution: Set<String> = emptySet()
) {

    /**
     * A type parameter is either bound to an actual type or to a star projection (which is no type at all
     * and therefore can only be recreated at type argument positions).
     */
    internal sealed interface Substitute {
        data object Star : Substitute {
            override fun toString(): String = "*"
        }

        class Actual(val type: KSType) : Substitute {
            override fun toString(): String = type.toString()
        }
    }

    companion object {
        val EMPTY = TypeSubstitution(emptyMap())

        /**
         * Determines the [TypeSubstitution] for all type parameters of the declaration of the given [type].
         */
        fun of(type: ResolvedType, resolver: Resolver): TypeSubstitution =
            of(type.declaration, type.ksType.arguments, resolver)

        /**
         * Zips the type parameters of the given [declaration] with the given [arguments].
         *
         * The [arguments] are resolved within the given [context], as they may themselves contain
         * typealiases or type parameters of an enclosing declaration.
         *
         * Type parameters without a matching type argument are not part of the resulting substitution and
         * will therefore be kept as they are.
         */
        internal fun of(
            declaration: KSDeclaration,
            arguments: List<KSTypeArgument>,
            resolver: Resolver,
            context: TypeSubstitution = EMPTY
        ): TypeSubstitution {
            val typeParameters = declaration.typeParameters
            if (typeParameters.isEmpty() || arguments.isEmpty()) return EMPTY

            val substitutions = typeParameters.mapIndexedNotNull { index, typeParameter ->
                val argument = arguments.getOrNull(index) ?: return@mapIndexedNotNull null
                val substitute = argument.substitute(resolver, context) ?: return@mapIndexedNotNull null
                typeParameter.key() to substitute
            }.toMap()

            return if (substitutions.isEmpty()) EMPTY else TypeSubstitution(substitutions)
        }

        private fun KSTypeArgument.substitute(resolver: Resolver, context: TypeSubstitution): Substitute? {
            if (this.variance == Variance.STAR) return Substitute.Star

            val argumentType = this.type?.resolve() ?: return null

            // the argument may itself be a type parameter which is bound to a star projection (e.g. with
            // `typealias Tags<T> = List<T>` and `typealias Boxes<T> = Tags<T>`, resolving `Boxes<*>`)
            if (context.isStar(argumentType.declaration)) return Substitute.Star

            return Substitute.Actual(argumentType.fullyResolved(resolver, context))
        }
    }

    internal fun typeOf(typeParameter: KSTypeParameter): KSType? =
        (substitutions[typeParameter.key()] as? Substitute.Actual)?.type

    internal fun isStar(declaration: KSDeclaration): Boolean =
        declaration is KSTypeParameter && substitutions[declaration.key()] === Substitute.Star

    internal fun isResolvingBoundOf(typeParameter: KSTypeParameter): Boolean =
        typeParameter.key() in boundsInResolution

    internal fun resolvingBoundOf(typeParameter: KSTypeParameter): TypeSubstitution =
        TypeSubstitution(substitutions, boundsInResolution + typeParameter.key())

    override fun toString(): String = substitutions.toString()

}

/**
 * Type parameters are only identifiable in combination with their declaring parent, as e.g. the `T` of
 * `class Tag<T>` is something completely different than the `T` of `typealias Tags<T> = List<Tag<T>>`.
 */
private fun KSTypeParameter.key(): String {
    val parent = this.parentDeclaration
    val parentName = parent?.qualifiedName?.asString()
        ?: parent?.simpleName?.asString()
        ?: parent.toString()
    return "$parentName#${this.name.asString()}"
}

