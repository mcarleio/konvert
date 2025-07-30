package io.mcarle.konvert.processor.exceptions

import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.NotAllowedParameterCombinationException

class InvalidMappingException(
    message: String,
    cause: Exception? = null
) : RuntimeException(message, cause) {

    companion object {
        fun missingSource(mappings: Iterable<Mapping>) =
            InvalidMappingException("The referenced source field(s) ${mappings.map { it.source }} do not exist")

        fun missingTarget(mappings: Iterable<Mapping>) =
            InvalidMappingException("The referenced target field(s) ${mappings.map { it.target }} do not exist")

        fun duplicateTarget(mappings: Iterable<Mapping>) =
            InvalidMappingException("There are multiple mappings for the same target: $mappings")

        fun incompatibleParameters(e: NotAllowedParameterCombinationException) = InvalidMappingException(e.message!!, e)
    }
}
