package io.mcarle.lib.kmapper.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.api.annotation.validate

fun Array<KMap>.validated(reference: KSNode, logger: KSPLogger) = filter { annotation ->
    var filterOutAnnotation = false
    annotation.validate { msg, filterOut ->
        logger.warn(msg, reference)
        filterOutAnnotation = filterOutAnnotation || filterOut
    }
    !filterOutAnnotation
}.also {
    groupBy { it.target }.onEach { (target, mappings) ->
        if (mappings.size > 1) {
            logger.warn("There are multiple mappings for target=$target", reference)
        }
    }
}