package io.mcarle.konvert.processor.exceptions

import io.mcarle.konvert.converter.api.config.IGNORED_TARGET_POLICY_OPTION

/**
 * @param sourcesByTarget every ignored target, mapped to a description of the source that would map to it
 */
class IgnoredTargetException(functionName: String, sourcesByTarget: Map<String, String>) : RuntimeException(
    "$functionName ignores target(s) that a source would map to: " +
        sourcesByTarget.entries.joinToString { (target, source) -> "'$target' from the $source" } + ". " +
        "Remove the @Mapping(target = \"...\", ignore = true), or map them " +
        "(${IGNORED_TARGET_POLICY_OPTION.key} is ERROR)"
)
