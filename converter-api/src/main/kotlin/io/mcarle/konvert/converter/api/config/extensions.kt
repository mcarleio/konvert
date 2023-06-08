package io.mcarle.konvert.converter.api.config

/**
 * @see ENFORCE_NOT_NULL_OPTION
 */
val Configuration.Companion.enforceNotNull: Boolean
    get() = ENFORCE_NOT_NULL_OPTION.get(CURRENT, String::toBoolean)

/**
 * @see KONVERTER_GENERATE_CLASS_OPTION
 */
val Configuration.Companion.konverterGenerateClass: Boolean
    get() = KONVERTER_GENERATE_CLASS_OPTION.get(CURRENT, String::toBoolean)

/**
 * @see GENERATED_FILENAME_SUFFIX_OPTION
 */
val Configuration.Companion.generatedFilenameSuffix: String
    get() = GENERATED_FILENAME_SUFFIX_OPTION.get(CURRENT) { it }

/**
 * @see ADD_GENERATED_KONVERTER_ANNOTATION_OPTION
 */
val Configuration.Companion.addGeneratedKonverterAnnotation: Boolean
    get() = ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.get(CURRENT, String::toBoolean)

/**
 * Reads the value for [Option.key] from the provided `options` or fallbacks to the [Option.defaultValue].
 */
inline fun <T> Option<T>.get(configuration: Configuration, mapping: (String) -> T): T {
    return configuration[this.key]?.let(mapping) ?: this.defaultValue
}
