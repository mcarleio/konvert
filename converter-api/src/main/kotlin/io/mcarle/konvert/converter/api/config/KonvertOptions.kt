package io.mcarle.konvert.converter.api.config

import io.mcarle.konvert.api.config.ENFORCE_NOT_NULL
import io.mcarle.konvert.api.config.GENERATED_FILENAME_SUFFIX
import io.mcarle.konvert.api.config.KONVERTER_GENERATE_CLASS

object KonvertOptions {
    val ENFORCE_NOT_NULL_OPTION = Option(ENFORCE_NOT_NULL, false)
    val KONVERTER_GENERATE_CLASS_OPTION = Option(KONVERTER_GENERATE_CLASS, false)
    val GENERATED_FILENAME_SUFFIX_OPTION = Option(GENERATED_FILENAME_SUFFIX, "Konverter")
}

data class Option<T>(val key: String, val defaultValue: T)
