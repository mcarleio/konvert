@file:Suppress("ClassName")

package io.mcarle.konvert.converter.api.config

import io.mcarle.konvert.api.TypeConverterName
import java.util.UUID

/**
 * Special handling for the case, that the source type is nullable and target type is not nullable:
 * When enabled, the converters will use the not-null assertion operator to enforce the mapped value to be non-null.
 * Otherwise, the converters should not match.
 *
 * Default: false
 */
object ENFORCE_NOT_NULL_OPTION : Option<Boolean>("konvert.enforce-not-null", false)

/**
 * Controls how Konvert enforces non-nullability when [ENFORCE_NOT_NULL_OPTION] is enabled.
 *
 * Possible values:
 * - "assertion-operator" (default)
 * - "require-not-null"
 *
 * @see EnforceNotNullStrategy
 * @since 4.x.x //TODO
 */
object ENFORCE_NOT_NULL_STRATEGY_OPTION : Option<EnforceNotNullStrategy>("konvert.enforce-not-null-strategy", EnforceNotNullStrategy.ASSERTION_OPERATOR)

/**
 * When set to true, a class instead of an object is being generated during processing of @[io.mcarle.konvert.api.Konverter]
 *
 * Default: false
 */
object KONVERTER_GENERATE_CLASS_OPTION : Option<Boolean>("konvert.konverter.generate-class", false)

/**
 * When set to true, Konvert will use reflection to find implementations of @Konverter annotated interfaces at runtime instead of
 * hard-coding them at ksp generation time.
 *
 * Default: false
 */
object KONVERTER_USE_REFLECTION_OPTION : Option<Boolean>("konvert.konverter.use-reflection", false)

/**
 * This setting will change the suffix for the generated filename from Konvert.
 *
 * Given the following examples:
 *
 * -
 *    ```kotlin
 *    @Konverter
 *    interface SomeMapper
 *    ```
 *    will generate a file named `SomeMapper${GENERATED_FILE_SUFFIX}.kt`
 *
 * -
 *    ```kotlin
 *    @KonvertTo(SomeTargetClass::class)
 *    class SomeSourceClass
 *    ```
 *    will generate a file named `SomeSourceClass${GENERATED_FILE_SUFFIX}.kt`
 *
 * -
 *    ```kotlin
 *    @KonvertFrom(SomeSourceClass::class)
 *    class SomeTargetClass { companion object }
 *    ```
 *    will generate a file named: `SomeTargetClass${GENERATED_FILE_SUFFIX}.kt`
 *
 *
 * Default: `Konverter`
 */
object GENERATED_FILENAME_SUFFIX_OPTION : Option<String>("konvert.generated-filename-suffix", "Konverter")

/**
 * This setting will, if enabled, add @[io.mcarle.konvert.api.GeneratedKonverter] to all generated functions
 *
 * Given:
 * ```kotlin
 * @KonvertTo(SomeTargetClass::class)
 * class SomeSourceClass
 * ```
 *
 * When enabled, will add the annotation like this:
 * ```kotlin
 * @GeneratedKonverter(priority = 3000)
 * fun SomeSourceClass.toSomeTargetClass() = SomeTargetClass()
 * ```
 *
 * Default: true
 */
object ADD_GENERATED_KONVERTER_ANNOTATION_OPTION : Option<Boolean>("konvert.add-generated-konverter-annotation", true)

/**
 * Enables the provided [io.mcarle.konvert.converter.api.TypeConverter]s, as some are not enabled by default
 * (see [io.mcarle.konvert.converter.api.TypeConverter.enabledByDefault]).
 *
 * See in package [io.mcarle.konvert.api.converter] for a list of provided type converter names.
 *
 * To enable multiple, you can pass them with either a comma or semikolon in between.
 *
 * Default: empty
 */
object ENABLE_CONVERTERS_OPTION : Option<List<TypeConverterName>>("konvert.enable-converters", emptyList())

/**
 * This setting defines the suffix for the generated module type.
 *
 * Any non word character will be replaced with an empty string.
 *
 * Default: random UUID
 */
object GENERATED_MODULE_SUFFIX_OPTION : Option<String>("konvert.generatedModuleSuffix", UUID.randomUUID().toString())

/**
 * This setting defines if the deprecated META-INF files should be parsed to load generated konverter functions.
 *
 * Will be removed in one of the next releases.
 *
 * Default: false
 */
object PARSE_DEPRECATED_META_INF_FILES_OPTION : Option<Boolean>("konvert.parseDeprecatedMetaInfFiles", false)

/**
 * Controls how properties outside the constructor (non-constructor properties and setters) are handled during mapping.
 *
 * Possible values:
 * - "auto" (default)
 * - "implicit"
 * - "explicit"
 * - "all"
 *
 * @see NonConstructorPropertiesMapping
 * @since 4.2.0
 */
object NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION : Option<NonConstructorPropertiesMapping>(
    key = "konvert.non-constructor-properties-mapping",
    defaultValue = NonConstructorPropertiesMapping.AUTO
)

/**
 * Controls how Konvert reacts when it encounters an invalid mapping.
 * A mapping is invalid when:
 * - it defines a source property that is not present
 * - it defines a target property that is not present
 * - it defines incompatible parameters (e.g. source and ignore=true)
 * - there are multiple mappings for the same target
 *
 * Possible values:
 * - "warn" (default)
 * - "fail"
 *
 * @see InvalidMappingStrategy
 * @since 4.2.0
 */
object INVALID_MAPPING_STRATEGY_OPTION : Option<InvalidMappingStrategy>(
    key = "konvert.invalid-mapping-strategy",
    defaultValue = InvalidMappingStrategy.WARN
)
