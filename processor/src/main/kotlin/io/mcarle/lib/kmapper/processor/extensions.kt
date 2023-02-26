package io.mcarle.lib.kmapper.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability
import io.mcarle.lib.kmapper.annotation.KMap
import io.mcarle.lib.kmapper.annotation.validate

fun KSType.isNullable(): Boolean {
    return this.isMarkedNullable || this.nullability == Nullability.NULLABLE || this.nullability == Nullability.PLATFORM
}

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