package io.mcarle.lib.kmapper.processor.shared

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.api.annotation.MoreThanOneParamDefinedException
import io.mcarle.lib.kmapper.api.annotation.NoParamDefinedException
import io.mcarle.lib.kmapper.api.annotation.validate
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import kotlin.reflect.KClass

fun Iterable<KMap>.validated(reference: KSNode, logger: KSPLogger) = filter { annotation ->
    try {
        annotation.validate()
        true
    } catch (e: NoParamDefinedException) {
        // Filter out, as it is not defined how the target field should be set
        logger.warn(e.message!!, reference)
        false
    } catch (e: MoreThanOneParamDefinedException) {
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

fun KMap.Companion.from(annotation: KSAnnotation) = KMap(
    target = annotation.arguments.first { it.name?.asString() == KMap::target.name }.value as String,
    source = annotation.arguments.first { it.name?.asString() == KMap::source.name }.value as String,
    constant = annotation.arguments.first { it.name?.asString() == KMap::constant.name }.value as String,
    expression = annotation.arguments.first { it.name?.asString() == KMap::expression.name }.value as String,
    ignore = annotation.arguments.first { it.name?.asString() == KMap::ignore.name }.value as Boolean,
    enable = (annotation.arguments.first { it.name?.asString() == KMap::enable.name }.value as List<*>)
        .filterIsInstance<KSType>()
        .map {
            Class.forName(it.declaration.qualifiedName!!.asString(), true, TypeConverter::class.java.classLoader).kotlin
        }
        .filterIsInstance<KClass<out TypeConverter>>()
        .toTypedArray(),
)