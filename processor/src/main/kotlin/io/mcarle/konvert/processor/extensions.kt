package io.mcarle.konvert.processor

import com.google.devtools.ksp.processing.KSPLogger
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
 * (`kspCommonMainKotlinMetadata`) KSP may omit arguments that were not explicitly set.
 * And [KSAnnotation.defaultArguments] is likewise unreliable there (array-valued defaults
 * such as `mappings = []` come back as `null`).
 *
 * We resolve in priority order: explicit argument -> KSP default argument -> [fallback].
 */
inline fun <reified T> KSAnnotation.argumentValue(name: String, fallback: T? = null): T {
    return arguments.firstOrNull { it.name?.asString() == name }?.value as? T
        ?: defaultArguments.firstOrNull { it.name?.asString() == name }?.value as? T
        ?: fallback
        ?: throw IllegalArgumentException("Could not resolve the default value for argument '$name' in annotation ${annotationType.resolve().declaration.simpleName.asString()}")
}

fun KSAnnotation.constructorArgClassDeclarations(name: String, unitType: KSType): List<KSClassDeclaration> {
    return argumentValue(name, listOf(unitType))
        .mapNotNull { it.classDeclaration() }
}

fun Mapping.Companion.from(annotation: KSAnnotation) = Mapping(
    target = annotation.argumentValue<String>(Mapping::target.name),
    source = annotation.argumentValue(Mapping::source.name, ""),
    constant = annotation.argumentValue(Mapping::constant.name, ""),
    expression = annotation.argumentValue(Mapping::expression.name, ""),
    ignore = annotation.argumentValue(Mapping::ignore.name, false),
    enable = (annotation.argumentValue(Mapping::enable.name, emptyList<Any?>()))
        .filterIsInstance<TypeConverterName>()
        .toTypedArray(),
)

fun Konfig.Companion.from(annotation: KSAnnotation) = Konfig(
    key = annotation.argumentValue<String>(Konfig::key.name),
    value = annotation.argumentValue<String>(Konfig::value.name)
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
