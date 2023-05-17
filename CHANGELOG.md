# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

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

### Breaking Changes for custom TypeConverter
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

[unreleased]: https://github.com/mcarleio/konvert/compare/v1.5.1...HEAD

[1.5.1]: https://github.com/mcarleio/konvert/compare/v1.5.0...v1.5.1

[1.5.0]: https://github.com/mcarleio/konvert/compare/v1.4.0...v1.5.0

[1.4.0]: https://github.com/mcarleio/konvert/compare/v1.3.0...v1.4.0

[1.3.0]: https://github.com/mcarleio/konvert/compare/v1.2.1...v1.3.0

[1.2.1]: https://github.com/mcarleio/konvert/compare/v1.2.0...v1.2.1

[1.2.0]: https://github.com/mcarleio/konvert/compare/v1.1.1...v1.2.0

[1.1.1]: https://github.com/mcarleio/konvert/compare/v1.1.0...v1.1.1

[1.1.0]: https://github.com/mcarleio/konvert/compare/v1.0.0...v1.1.0

[1.0.0]: https://github.com/mcarleio/konvert/releases/tag/v1.0.0
