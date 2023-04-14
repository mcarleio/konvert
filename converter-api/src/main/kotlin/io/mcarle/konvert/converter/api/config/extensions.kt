package io.mcarle.konvert.converter.api.config

/**
 * Special handling for the case, that the source type is nullable and target type is not nullable:
 * When enabled, the converters will use the not-null assertion operator to enforce the mapped value to be non-null.
 * Otherwise, the converters should not match.
 */
val Configuration.Companion.enforceNotNull: Boolean
    get() = KonvertOptions.ENFORCE_NOT_NULL.get(CURRENT, String::toBoolean)
val Configuration.Companion.konverterGenerateClass: Boolean
    get() = KonvertOptions.KONVERTER_GENERATE_CLASS.get(CURRENT, String::toBoolean)

/**
 * Reads the value for [Option.key] from the provided `options` or fallbacks to the [Option.defaultValue].
 */
inline fun <T> Option<T>.get(configuration: Configuration, mapping: (String) -> T): T {
    return configuration[this.key]?.let(mapping) ?: this.defaultValue
}
