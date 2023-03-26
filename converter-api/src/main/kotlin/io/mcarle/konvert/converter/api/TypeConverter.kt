package io.mcarle.konvert.converter.api

import com.google.devtools.ksp.symbol.KSType

/**
 * All classes, which should be used by Konverter during KSP to identify a conversion from one to another type, have to implement this interface.
 * During Konverter startup, the [java.util.ServiceLoader] is used to collect all implementations on the classpath.
 */
interface TypeConverter {

    /**
     * Not all converters may be suitable for every use case: For example a conversion of a String to an Int may not always be possible.
     * Therefore, [io.mcarle.konvert.converter.StringToIntConverter] is not enabled by default.
     */
    val enabledByDefault: Boolean

    /**
     * Used to sort all the available converters, as the first matching converter will be used.
     * This enables to e.g. override a specific converter by defining a lower value here.
     */
    val priority: Priority get() = DEFAULT_PRIORITY

    /**
     * Used to initialise all TypeConverters in the beginning of the KSP with the resolver and the provided configurations.
     */
    fun init(config: io.mcarle.konvert.converter.api.ConverterConfig)

    /**
     * Used to check, if this type converter is able to do the conversion from `source` to `target`
     */
    fun matches(source: KSType, target: KSType): Boolean

    /**
     * Called to generate the kotlin code to convert source to from.
     *
     * @param fieldName the property name, which should be converted
     */
    fun convert(fieldName: String, source: KSType, target: KSType): String
}
