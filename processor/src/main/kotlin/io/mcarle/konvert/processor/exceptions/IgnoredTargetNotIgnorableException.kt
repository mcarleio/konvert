package io.mcarle.konvert.processor.exceptions

class IgnoredTargetNotIgnorableException(target: String) : RuntimeException(
    "The target `$target` is not ignorable"
)
