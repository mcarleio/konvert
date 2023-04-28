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
const val GENERATED_FILENAME_SUFFIX = "konvert.generated-filename-suffix"
