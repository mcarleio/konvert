package io.mcarle.konvert.processor.exceptions

class NotAssignableTargetException(targets: List<String>) : RuntimeException(
    "The target properties ${targets.joinToString { "`$it`" }} are not assignable: " +
        "a var whose setter is private, protected or internal to another module cannot be written by the generated code"
)
