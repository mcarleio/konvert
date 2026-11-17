package io.mcarle.konvert.converter.api.config

/**
 * Defines how Konvert reports a target constructor parameter or assignable non-constructor property that no source
 * property and no mapping provides.
 *
 * @see IGNORE
 * @see WARN
 * @see ERROR
 */
enum class UnmappedTargetPolicy {

    /**
     * The parameter's default value is used, or `null` for a nullable parameter without one, and the property keeps the
     * value the constructor gives it. Nothing is reported.
     */
    IGNORE,

    /**
     * As [IGNORE], but every such parameter and property is logged as a warning.
     */
    WARN,

    /**
     * Fail with an exception naming every such parameter and property.
     */
    ERROR
}
