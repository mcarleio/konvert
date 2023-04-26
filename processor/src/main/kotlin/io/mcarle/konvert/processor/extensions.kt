package io.mcarle.konvert.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.NoParamDefinedException
import io.mcarle.konvert.api.NotAllowedParameterCombinationException
import io.mcarle.konvert.api.TypeConverterName
import io.mcarle.konvert.api.validate
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.classDeclaration
import kotlin.reflect.KClass

fun Iterable<Mapping>.validated(reference: KSNode, logger: KSPLogger) = filter { annotation ->
    try {
        annotation.validate()
        true
    } catch (e: NoParamDefinedException) {
        // Filter out, as it is not defined how the target field should be set
        logger.warn(e.message!!, reference)
        false
    } catch (e: NotAllowedParameterCombinationException) {
        // Only warn
        logger.warn(e.message!!, reference)
        true
    }
}.also {
    groupBy { it.target }.onEach { (target, mappings) ->
        if (mappings.size > 1) {
            logger.warn("Multiple mappings for target=$target", reference)
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
