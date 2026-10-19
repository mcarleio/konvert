package io.mcarle.konvert.processor.exceptions

/**
 * @param candidates the qualified source paths that would all map to [target], e.g. `entity.status`
 */
class AmbiguousSourceException(target: String, candidates: List<String>) : RuntimeException(
    "The target '$target' matches several sources: $candidates. " +
        "Choose one with @Mapping(target = \"$target\", source = \"${candidates.last()}\"), or map it otherwise"
)
