package io.mcarle.konvert.converter.api.config

/**
 * Defines how Konvert reports a target constructor parameter that no source property and no mapping provides.
 *
 * @see IGNORE
 * @see WARN
 * @see ERROR
 */
enum class UnmappedTargetPolicy {

    /**
     * The parameter's default value is used, or `null` for a nullable parameter without one. Nothing is reported.
     */
    IGNORE,

    /**
     * As [IGNORE], but every such parameter is logged as a warning.
     */
    WARN,

    /**
     * Fail with an exception naming every such parameter.
     */
    ERROR
}
