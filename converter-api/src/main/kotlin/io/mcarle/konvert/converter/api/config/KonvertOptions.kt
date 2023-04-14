package io.mcarle.konvert.converter.api.config

object KonvertOptions {
    val ENFORCE_NOT_NULL = Option("konvert.enforce-not-null", false)
    val KONVERTER_GENERATE_CLASS = Option("konvert.konverter.generate-class", false)
}

data class Option<T>(val key: String, val defaultValue: T)
