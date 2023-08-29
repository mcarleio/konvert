# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [2.2.0]

Update to Kotlin 1.9.0 and KSP 1.0.12

### Bug fixes
* ignore missing source property for a target constructor parameter with a default value [#16](https://github.com/mcarleio/konvert/issues/16)
  ```kotlin
  @KonvertTo(Target::class)
  data class Source(val property: String)
  data class Target(val property: String, val withDefaultVal: Int = 42)
  ```

## [2.1.0]

### New features
* override implemented interface functions (simply calling `super`) to add the `@GeneratedKonverter` annotation
  * this enables to load them on startup when processing entries in `META-INF/io.mcarle.konvert.api.Konvert`

### Bug fixes
* add `Impl` suffix to classes in `META-INF/io.mcarle.konvert.api.Konvert` (i.e. do not reference the interface, but the implementing class/object)

## [2.0.0]

### New features
* call own functions of a `@Konverter` annotated interface directly instead of calling `Konverter.get` [[c828f05](https://github.com/mcarleio/konvert/commit/c828f0594c8d660726fc1eb9fa083459fc94e3af)]
* functions in `@Konverter` are now allowed to have a nullable source parameter [[4bf1c97](https://github.com/mcarleio/konvert/commit/4bf1c974fd51bdd341a5f17078cfd45b982c83ef)]
* use more imports instead of fully qualified names [[1a40238](https://github.com/mcarleio/konvert/commit/1a40238138728fd390e6a60c670bbd5579e9156e)]
* enable (re-) usage of generated konverter from other modules or libraries [[5d358ea](https://github.com/mcarleio/konvert/commit/5d358ea1f986b3fa78e72a4bcf1a1536471c5d24)]
  * therefore, generate META-INF files containing all generated functions
  * add `@GeneratedKonverter` annotation to all generated functions
  * and add a new option `konvert.add-generated-konverter-annotation` to disable this feature

### Breaking Changes for custom `TypeConverter`
* changed return type of the function `convert` from `String` to `CodeBlock` [[cc3cadd](https://github.com/mcarleio/konvert/commit/cc3caddca7323e1ff8fad0df4e5944e75b86ad2c)]
  * The simplest migration is to wrap your result `String` in a `CodeBlock` like this:
    ```kotlin
    fun convert(fieldName: String, source: KSType, target: KSType): CodeBlock {
        // ... do your stuff ...
        return CodeBlock.of("your conversion code")
    }
    ```

## [1.5.1]

### Bug fixes
* actually use the collected classloaders from 1.5.0... [#13](https://github.com/mcarleio/konvert/issues/13)

## [1.5.0]

### New features
* add new type converters for `BigInteger` and `BigDecimal`
* add options for `koin-injector`: `konvert.koin.default-injection-method` and `konvert.koin.default-scope` (thanks to [@jakoss](https://github.com/jakoss) [#6](https://github.com/mcarleio/konvert/pull/6))
* add new injector for `anvil` (thanks to [@jakoss](https://github.com/jakoss) [#8](https://github.com/mcarleio/konvert/pull/8))

### Bug fixes
* fixed class loading issues of generated interface implementations during runtime [#13](https://github.com/mcarleio/konvert/issues/13)

## [1.4.0]

### New features
* add new modules:
  * The module `plugin-api` can be used to extend the `processor` at certain points
  * The modules `injectors/spring-injector` and `injectors/cdi-injector` which use the `plugin-api` to
    add annotations like `@Component` or `@RequestScoped` to the generated types from `@Konverter`
    * The modules `injectors/spring-annotations` and `injectors/cdi-annotations` so that the user can define the exact annotations
  * The modules `injectors/koin-injector` and `injectors/koin-annotations` (thanks to [@jakoss](https://github.com/jakoss) [#4](https://github.com/mcarleio/konvert/pull/4))
* add new option `konvert.generated-filename-suffix` to define the trailing part of the generated filename

### Bug fixes
* defined `options` in `@KonvertTo` and `@KonvertFrom` are now used

## [1.3.0]

### New features
* reimplemented configuration handling
   * new annotation `@Konfig` to define configurations
   * isolated configurations per converter
* new option `konvert.konverter.generate-class` to generate a `class` instead of an `object` on `@Konverter` (can be set globally or
  per `@Konverter` via `@Konfig`)
   * the `Konverter.get<YOUR_INTERFACE>()` implementation is extended to instantiate an instance of a class

### Breaking Changes for custom `TypeConverter`
* changed signature of `TypeConverter.init` to only pass the resolver
   * removed `options` field from `AbstractTypeConverter`
* changed option key `enforce-not-null` to `konvert.enforce-not-null`

### Bug fixes
* handle nullable parameter in mapping functions of a `@Konverter`

## [1.2.1]

### Bug fixes
* use FQN when converting between enums in different packages
* only create an import alias if not nullable type is not equal to the type reference

## [1.2.0]

### New features
* If an import alias was used for `@Konvert`, the generated object will also use that alias

### Bug fixes
* fix the special case when source and target class both have the same class name
* use non-breaking space to prevent rare cases where a line break could break code

## [1.1.1]

### Bug fixes

* the nullability checks for `KonvertTypeConverter` now consider both, declared and current type for source and target

## [1.1.0]

### Breaking Changes

* enabled by default following converters:
   * `ULongToNumberConverter`
   * `ULongToDoubleConverter`
   * `UShortToNumberConverter`
   * `UByteToNumberConverter`
* changed outcome of following converters: `ULongToNumberConverter`
* renamed the following converters:
   * `LongToDateConverter` -> `LongEpochMillisToDateConverter`
   * `DateToLongConverter` -> `DateToLongEpochMillisConverter`
   * `InstantToLongConverter` -> `InstantToLongEpochMillisConverter`
   * `ZonedDateTimeToLongConverter` -> `ZonedDateTimeToLongEpochMillisConverter`
   * `OffsetDateTimeToLongConverter` -> `OffsetDateTimeToLongEpochMillisConverter`
   * `LongToInstantConverter` -> `LongEpochMillisToInstantConverter`
* moved from `converter` to `converter-api` module:
   * `AbstractTypeConverter`
   * `ConverterOptions`

### New features

* implemented new converters:
   * `LongEpochSecondsToDateConverter`
   * `DateToLongEpochSecondsConverter`
   * `InstantToLongEpochSecondsConverter`
   * `ZonedDateTimeToLongEpochSecondsConverter`
   * `OffsetDateTimeToLongEpochSecondsConverter`
   * `OffsetTimeToStringConverter`
   * `LocalTimeToStringConverter`
   * `StringToOffsetTimeConverter`
   * `StringToLocalTimeConverter`
   * `LongEpochSecondsToInstantConverter`
   * `OffsetDateTimeToInstantConverter`
   * `OffsetDateTimeToZonedDateTimeConverter`
   * `OffsetDateTimeToLocalDateTimeConverter`
   * `OffsetDateTimeToLocalDateConverter`
   * `OffsetDateTimeToLocalTimeConverter`
   * `OffsetDateTimeToOffsetTimeConverter`
   * `ZonedDateTimeToInstantConverter`
   * `ZonedDateTimeToOffsetDateTimeConverter`
   * `ZonedDateTimeToLocalDateTimeConverter`
   * `ZonedDateTimeToLocalDateConverter`
   * `ZonedDateTimeToLocalTimeConverter`
   * `ZonedDateTimeToOffsetTimeConverter`
   * `LocalDateTimeToLocalDateConverter`
   * `LocalDateTimeToLocalTimeConverter`
   * `OffsetTimeToLocalTimeConverter`

### Bug fixes

* Types with generics on a function in interfaces annotated with `Konverter` work
* The generated `TypeConverter` for `KonvertTo`, `KonvertFrom` and `Konvert` can handle nullable source with not nullable target

## [1.0.0] - 2023-03-27

[unreleased]: https://github.com/mcarleio/konvert/compare/v2.2.0...HEAD

[2.2.0]: https://github.com/mcarleio/konvert/compare/v2.1.0...v2.2.0

[2.1.0]: https://github.com/mcarleio/konvert/compare/v2.0.0...v2.1.0

[2.0.0]: https://github.com/mcarleio/konvert/compare/v1.5.1...v2.0.0

[1.5.1]: https://github.com/mcarleio/konvert/compare/v1.5.0...v1.5.1

[1.5.0]: https://github.com/mcarleio/konvert/compare/v1.4.0...v1.5.0

[1.4.0]: https://github.com/mcarleio/konvert/compare/v1.3.0...v1.4.0

[1.3.0]: https://github.com/mcarleio/konvert/compare/v1.2.1...v1.3.0

[1.2.1]: https://github.com/mcarleio/konvert/compare/v1.2.0...v1.2.1

[1.2.0]: https://github.com/mcarleio/konvert/compare/v1.1.1...v1.2.0

[1.1.1]: https://github.com/mcarleio/konvert/compare/v1.1.0...v1.1.1

[1.1.0]: https://github.com/mcarleio/konvert/compare/v1.0.0...v1.1.0

[1.0.0]: https://github.com/mcarleio/konvert/releases/tag/v1.0.0
