@file:Suppress("ClassName")

package io.mcarle.konvert.converter.api.config

import io.mcarle.konvert.api.TypeConverterName

/**
 * Special handling for the case, that the source type is nullable and target type is not nullable:
 * When enabled, the converters will use the not-null assertion operator to enforce the mapped value to be non-null.
 * Otherwise, the converters should not match.
 *
 * Default: false
 */
object ENFORCE_NOT_NULL_OPTION : Option<Boolean>("konvert.enforce-not-null", false)

/**
 * When set to true, a class instead of an object is being generated during processing of @[io.mcarle.konvert.api.Konverter]
 *
 * Default: false
 */
object KONVERTER_GENERATE_CLASS_OPTION : Option<Boolean>("konvert.konverter.generate-class", false)

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
