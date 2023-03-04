package io.mcarle.lib.kmapper.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.api.annotation.MoreThanOneParamDefinedException
import io.mcarle.lib.kmapper.api.annotation.NoParamDefinedException
import io.mcarle.lib.kmapper.api.annotation.validate

fun Array<KMap>.validated(reference: KSNode, logger: KSPLogger) = filter { annotation ->
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