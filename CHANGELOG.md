# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

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

[unreleased]: https://github.com/mcarleio/konvert/compare/v1.1.1...HEAD

[1.1.1]: https://github.com/mcarleio/konvert/compare/v1.1.0...v1.1.1

[1.1.0]: https://github.com/mcarleio/konvert/compare/v1.0.0...v1.1.0

[1.0.0]: https://github.com/mcarleio/konvert/releases/tag/v1.0.0
