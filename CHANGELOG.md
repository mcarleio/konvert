# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

#### Potential breaking

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

#### New features

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

## [1.0.0] - 2023-03-27

[unreleased]: https://github.com/mcarleio/konvert/compare/v1.0.0...HEAD

[1.0.0]: https://github.com/mcarleio/konvert/releases/tag/v1.0.0
