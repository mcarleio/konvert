package io.mcarle.konvert.converter.api

@JvmInline
value class Options(val options: Map<String, String>)


interface Option {
    val configKey: String
    val defaultValue: Any?
}

/**
 * Reads the value for [Option.configKey] from the provided `options` or fallbacks to the [Option.defaultValue].
 * Returns that value after checking its type against [T], fallbacks to null.
 */
inline fun <reified T> io.mcarle.konvert.converter.api.Option.get(options: io.mcarle.konvert.converter.api.Options): T? {
    // Hint: `T` needs to be reified to ensure `as?` does not throw ClassCastException errors
    return (options.options[this.configKey]?.toBoolean() ?: this.defaultValue) as? T
}
