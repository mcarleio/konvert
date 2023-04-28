package io.mcarle.konvert.converter.api.config

/**
 * @see io.mcarle.konvert.api.config.ENFORCE_NOT_NULL
 */
val Configuration.Companion.enforceNotNull: Boolean
    get() = KonvertOptions.ENFORCE_NOT_NULL_OPTION.get(CURRENT, String::toBoolean)

/**
 * @see io.mcarle.konvert.api.config.KONVERTER_GENERATE_CLASS
 */
val Configuration.Companion.konverterGenerateClass: Boolean
    get() = KonvertOptions.KONVERTER_GENERATE_CLASS_OPTION.get(CURRENT, String::toBoolean)

/**
 * @see io.mcarle.konvert.api.config.GENERATED_FILENAME_SUFFIX
 */
val Configuration.Companion.generatedFilenameSuffix: String
    get() = KonvertOptions.GENERATED_FILENAME_SUFFIX_OPTION.get(CURRENT) { it }

/**
 * Reads the value for [Option.key] from the provided `options` or fallbacks to the [Option.defaultValue].
 */
inline fun <T> Option<T>.get(configuration: Configuration, mapping: (String) -> T): T {
    return configuration[this.key]?.let(mapping) ?: this.defaultValue
}
