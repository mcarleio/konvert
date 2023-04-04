package io.mcarle.konvert.converter.api.config

import io.mcarle.konvert.converter.api.Option
import io.mcarle.konvert.converter.api.Options
import io.mcarle.konvert.converter.api.get

/**
 * Special handling for the case, that the source type is nullable and target type is not nullable:
 * When enabled, the converters will use the not-null assertion operator to enforce the mapped value to be non-null.
 * Otherwise, the converters should not match.
 */
val Options.enforceNotNull: Boolean get() = ConverterOptions.ENFORCE_NOT_NULL.get(this) ?: false

internal enum class ConverterOptions(override val configKey: String, override val defaultValue: Any? = null) : Option {

    /**
     * @see Options.enforceNotNull
     */
    ENFORCE_NOT_NULL("enforce-not-null", false),
    ;

}
