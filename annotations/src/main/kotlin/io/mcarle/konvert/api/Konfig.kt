package io.mcarle.konvert.api

@Retention(AnnotationRetention.SOURCE)
annotation class Konfig(
    /**
     * Name/Key of an option. See in package [io.mcarle.konvert.api.config] for a list of options.
     */
    val key: String,
    /**
     * Value for the defined option
     */
    val value: String
) {
    companion object
}
