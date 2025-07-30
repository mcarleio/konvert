package io.mcarle.konvert.converter.api.config

/**
 * Defines how Konvert should handle invalid mappings.
 *
 * @see WARN
 * @see FAIL
 */
enum class InvalidMappingStrategy {

    /**
     * Logs a warning when an invalid mapping is encountered. Ignores that mapping and continues execution.
     */
    WARN,

    /**
     *
     * Fail with an exception when an invalid mapping is encountered.
     */
    FAIL
}
