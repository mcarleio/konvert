package io.mcarle.konvert.api.config

/**
 * Special handling for the case, that the source type is nullable and target type is not nullable:
 * When enabled, the converters will use the not-null assertion operator to enforce the mapped value to be non-null.
 * Otherwise, the converters should not match.
 *
 * Default: false
 */
const val ENFORCE_NOT_NULL = "konvert.enforce-not-null"

/**
 * When set to true, a class instead of an object is being generated during processing of @[io.mcarle.konvert.api.Konverter]
 *
 * Default: false
 */
const val KONVERTER_GENERATE_CLASS = "konvert.konverter.generate-class"
