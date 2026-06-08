package io.mcarle.konvert.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
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

/**
 * Resolves the value of an annotation argument by [name].
 *
 * On platform KSP compilations every argument (including ones left at their default) is
 * materialized in [KSAnnotation.arguments]. In the common-metadata compilation
 * (`kspCommonMainKotlinMetadata`) KSP may omit arguments that were not explicitly set —
 * and [KSAnnotation.defaultArguments] is likewise unreliable there (array-valued defaults
 * such as `mappings = []` come back as `null`). A naive
 * `arguments.first { it.name == name }.value` therefore throws [NoSuchElementException], and
 * even after guarding the lookup the value may be `null`.
 *
 * We resolve in priority order: explicit argument → KSP default argument → [fallback].
 *
 * Note on testing: the metadata-phase behavior this guards against cannot be reproduced in
 * the `*ITest` harness, which runs a JVM platform compilation via kotlin-compile-testing
 * (no commonMain-metadata KSP mode exists there). This function is therefore unit-tested
 * directly against a fake [KSAnnotation] simulating the metadata phase — see
 * `AnnotationArgumentExtensionsTest`.
 */
fun KSAnnotation.argumentValue(name: String, fallback: Any? = null): Any? {
    arguments.firstOrNull { it.name?.asString() == name }?.let { if (it.value != null) return it.value }
    defaultArguments.firstOrNull { it.name?.asString() == name }?.let { if (it.value != null) return it.value }
    return fallback
}

/**
 * Resolves the `constructorArgs` annotation value into [KSClassDeclaration]s.
 *
 * Two states must be distinguished, which is impossible from the value alone in the
 * common-metadata compilation (where a defaulted argument is dropped from
 * [KSAnnotation.arguments]):
 *  - **absent** (left at its `[Unit::class]` default) → the "auto-detect constructor" sentinel.
 *    We synthesize `[Unit]` so downstream behaves exactly as in a platform compilation.
 *  - **explicitly empty** (`constructorArgs = []`) → "use the empty/no-arg constructor".
 *    We must preserve the empty list.
 *
 * We treat the argument as explicitly set only when it appears in [KSAnnotation.arguments]
 * with a non-null value; otherwise we fall back to the [Unit] sentinel.
 *
 * Note on testing: the absent-vs-explicit-empty distinction only diverges in the
 * common-metadata compilation. In the JVM `*ITest` harness a defaulted `constructorArgs`
 * always arrives as `[Unit::class]`, so those integration tests exercise the unchanged
 * platform path and cannot prove this fix. Faithful coverage would require running
 * `kspCommonMainKotlinMetadata` (and, for native consumers, macOS runners with the
 * Kotlin/Native toolchain), neither of which is available to the integration harness.
 */
fun KSAnnotation.constructorArgClassDeclarations(name: String, resolver: Resolver): List<KSClassDeclaration> {
    val explicit = arguments.firstOrNull { it.name?.asString() == name }?.value
    if (explicit != null) {
        return (explicit as List<*>).mapNotNull { (it as? KSType)?.classDeclaration() }
    }
    val unit = resolver.getClassDeclarationByName(Unit::class.qualifiedName!!)
    return listOfNotNull(unit)
}

fun Mapping.Companion.from(annotation: KSAnnotation) = Mapping(
    target = annotation.argumentValue(Mapping::target.name) as String,
    source = annotation.argumentValue(Mapping::source.name, "") as String,
    constant = annotation.argumentValue(Mapping::constant.name, "") as String,
    expression = annotation.argumentValue(Mapping::expression.name, "") as String,
    ignore = annotation.argumentValue(Mapping::ignore.name, false) as Boolean,
    enable = (annotation.argumentValue(Mapping::enable.name, emptyList<Any?>()) as List<*>)
        .filterIsInstance<TypeConverterName>()
        .toTypedArray(),
)

fun Konfig.Companion.from(annotation: KSAnnotation) = Konfig(
    key = annotation.argumentValue(Konfig::key.name) as String,
    value = annotation.argumentValue(Konfig::value.name) as String
)

fun KSValueParameter.typeClassDeclaration(): KSClassDeclaration? = this.type.resolve().classDeclaration()

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
