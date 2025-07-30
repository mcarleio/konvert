package io.mcarle.konvert.converter.api.config

/**
 * Defines how non-constructor target properties should be handled during mapping.
 *
 * @see AUTO
 * @see EXPLICIT
 * @see IMPLICIT
 * @see ALL
 */
enum class NonConstructorPropertiesMapping {

    /**
     * Behaves like [IMPLICIT] if no [io.mcarle.konvert.api.Mapping]s are present
     * (other than those with [io.mcarle.konvert.api.Mapping.ignore]`=true`).
     * Otherwise, behaves like [EXPLICIT].
     */
    AUTO,

    /**
     * Only non-constructor target properties that are explicitly declared within [io.mcarle.konvert.api.Mapping]s will be mapped.
     * Ignores the rest, even if they have matching source properties.
     */
    EXPLICIT,

    /**
     * Generates mappings for every non-constructor target property for which a matching source property exist or a
     * [io.mcarle.konvert.api.Mapping] is defined.
     * Ignores the rest.
     */
    IMPLICIT,

    /**
     * All non-constructor target properties must be mapped. Throws exceptions,
     * if no matching source property/[io.mcarle.konvert.api.Mapping] is found/defined.
     */
    ALL,
}
