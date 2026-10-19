package io.mcarle.konvert.converter.api.config

/**
 * Defines how Konvert reports a target excluded with `@Mapping(target = "...", ignore = true)` that a source property
 * or a parameter of the mapping function would map to by name.
 *
 * @see IGNORE
 * @see WARN
 * @see ERROR
 */
enum class IgnoredTargetPolicy {

    /**
     * The target is ignored. Nothing is reported.
     */
    IGNORE,

    /**
     * As [IGNORE], but every such target is logged as a warning.
     */
    WARN,

    /**
     * Fail with an exception naming every such target.
     */
    ERROR
}
