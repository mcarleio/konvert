package io.mcarle.konvert.converter.api.config

import io.mcarle.konvert.api.TypeConverterName

/**
 * @see ENFORCE_NOT_NULL_OPTION
 */
val Configuration.Companion.enforceNotNull: Boolean
    get() = ENFORCE_NOT_NULL_OPTION.get(CURRENT, String::toBoolean)

/**
 * @see ENFORCE_NOT_NULL_STRATEGY_OPTION
 */
val Configuration.Companion.enforceNotNullStrategy: EnforceNotNullStrategy
    get() = ENFORCE_NOT_NULL_STRATEGY_OPTION.get(CURRENT) { configString ->
        EnforceNotNullStrategy.entries.firstOrNull {
            it.name.equals(configString, ignoreCase = true)
        } ?: ENFORCE_NOT_NULL_STRATEGY_OPTION.defaultValue
    }

/**
 * @see KONVERTER_GENERATE_CLASS_OPTION
 */
val Configuration.Companion.konverterGenerateClass: Boolean
    get() = KONVERTER_GENERATE_CLASS_OPTION.get(CURRENT, String::toBoolean)

/**
 * @see KONVERTER_USE_REFLECTION_OPTION
 */
val Configuration.Companion.konverterUseReflection: Boolean
    get() = KONVERTER_USE_REFLECTION_OPTION.get(CURRENT, String::toBoolean)

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
 * @see ENABLE_CONVERTERS_OPTION
 */
val Configuration.Companion.enableConverters: List<TypeConverterName>
    get() = ENABLE_CONVERTERS_OPTION.get(CURRENT) { configString -> configString.split(";", ",").map { it.trim() } }

/**
 * @see GENERATED_MODULE_SUFFIX_OPTION
 */
val Configuration.Companion.generatedModuleSuffix: String
    get() = GENERATED_MODULE_SUFFIX_OPTION.get(CURRENT) { it.replace("\\W".toRegex(), "") }

/**
 * @see PARSE_DEPRECATED_META_INF_FILES_OPTION
 */
val Configuration.Companion.parseDeprecatedMetaInfFiles: Boolean
    get() = PARSE_DEPRECATED_META_INF_FILES_OPTION.get(CURRENT, String::toBoolean)

/**
 * @see NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION
 */
val Configuration.Companion.nonConstructorPropertiesMapping: NonConstructorPropertiesMapping
    get() = NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.get(CURRENT) { configString ->
        NonConstructorPropertiesMapping.entries.firstOrNull { it.name.equals(configString, ignoreCase = true) }
            ?: NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.defaultValue
    }

/**
 * @see INVALID_MAPPING_STRATEGY_OPTION
 */
val Configuration.Companion.invalidMappingStrategy: InvalidMappingStrategy
    get() = INVALID_MAPPING_STRATEGY_OPTION.get(CURRENT) { configString ->
        InvalidMappingStrategy.entries.firstOrNull { it.name.equals(configString, ignoreCase = true) }
            ?: INVALID_MAPPING_STRATEGY_OPTION.defaultValue
    }

/**
 * Reads the value for [Option.key] from the provided `options` or fallbacks to the [Option.defaultValue].
 */
inline fun <T> Option<T>.get(configuration: Configuration, mapping: (String) -> T): T {
    return configuration[this.key]?.let(mapping) ?: this.defaultValue
}
