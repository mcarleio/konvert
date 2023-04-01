---
layout: default
title: Provided `TypeConverter`s
nav_order: 3
---

`Konvert` comes with a set of default `TypeConverter`s.

The following tables share this legend:

* `✔` = `TypeConverter` exists and is enabled by default
* `☑` = `TypeConverter` exists but is not enabled by default
* `-` = no `TypeConverter` existing

## Basic Types

| From\To | String | Int | UInt | Long | ULong | Short | UShort | Number | Byte | UByte | Char | Boolean | Float | Double |
|---------|:------:|:---:|:----:|:----:|:-----:|:-----:|:------:|:------:|:----:|:-----:|:----:|:-------:|:-----:|:------:|
| String  |   ✔    |  ☑  |  ☑   |  ☑   |   ☑   |   ☑   |   ☑    |   ☑    |  ☑   |   ☑   |  ☑   |    ☑    |   ☑   |   ☑    |
| Int     |   ✔    |  ✔  |  ☑   |  ✔   |   ☑   |   ☑   |   ☑    |   ✔    |  ☑   |   ☑   |  ☑   |    ☑    |   ✔   |   ✔    |
| UInt    |   ✔    |  ☑  |  ✔   |  ✔   |   ✔   |   ☑   |   ☑    |   ✔    |  ☑   |   ☑   |  ☑   |    ☑    |   ✔   |   ✔    |
| Long    |   ✔    |  ☑  |  ☑   |  ✔   |   ☑   |   ☑   |   ☑    |   ✔    |  ☑   |   ☑   |  ☑   |    ☑    |   ☑   |   ✔    |
| ULong   |   ✔    |  ☑  |  ☑   |  ☑   |   ✔   |   ☑   |   ☑    |   ✔    |  ☑   |   ☑   |  ☑   |    ☑    |   ☑   |   ✔    |
| Short   |   ✔    |  ✔  |  ☑   |  ✔   |   ☑   |   ✔   |   ☑    |   ✔    |  ☑   |   ☑   |  ☑   |    ☑    |   ✔   |   ✔    |
| UShort  |   ✔    |  ✔  |  ✔   |  ✔   |   ✔   |   ☑   |   ✔    |   ✔    |  ☑   |   ☑   |  ☑   |    ☑    |   ✔   |   ✔    |
| Number  |   ✔    |  ☑  |  ☑   |  ☑   |   ☑   |   ☑   |   ☑    |   ✔    |  ☑   |   ☑   |  ☑   |    ☑    |   ☑   |   ☑    |
| Byte    |   ✔    |  ✔  |  ☑   |  ✔   |   ☑   |   ✔   |   ☑    |   ✔    |  ✔   |   ☑   |  ☑   |    ☑    |   ✔   |   ✔    |
| UByte   |   ✔    |  ✔  |  ✔   |  ✔   |   ✔   |   ✔   |   ✔    |   ✔    |  ☑   |   ✔   |  ☑   |    ☑    |   ✔   |   ✔    |
| Char    |   ✔    |  ☑  |  ☑   |  ☑   |   ☑   |   ☑   |   ☑    |   ☑    |  ☑   |   ☑   |  ✔   |    ☑    |   ☑   |   ☑    |
| Boolean |   ✔    |  ☑  |  ☑   |  ☑   |   ☑   |   ☑   |   ☑    |   ☑    |  ☑   |   ☑   |  ☑   |    ✔    |   ☑   |   ☑    |
| Float   |   ✔    |  ☑  |  ☑   |  ☑   |   ☑   |   ☑   |   ☑    |   ✔    |  ☑   |   ☑   |  ☑   |    ☑    |   ✔   |   ✔    |
| Double  |   ✔    |  ☑  |  ☑   |  ☑   |   ☑   |   ☑   |   ☑    |   ✔    |  ☑   |   ☑   |  ☑   |    ☑    |   ☑   |   ✔    |

Hint: All `TypeConverter` for basic types are enabled by default, if for each source value, when you convert it to target type and then back
to source type, the value is the same as the source value.

## Temporal Types

| From\To         | Date | Instant | ZonedDateTime | OffsetDateTime | LocalDateTime | LocalDate | OffsetTime | LocalTime |
|-----------------|:----:|:-------:|:-------------:|:--------------:|:-------------:|:---------:|:----------:|:---------:|
| String          |  -   |    ☑    |       ☑       |       ☑        |       ☑       |     ☑     |     ☑      |     ☑     |
| Long (Epoch ms) |  ☑   |    ☑    |       -       |       -        |       -       |     -     |     -      |     -     |
| Long (Epoch s)  |  ☑   |    ☑    |       -       |       -        |       -       |     -     |     -      |     -     |
| Date            |  ✔   |    ✔    |       -       |       -        |       -       |     -     |     -      |     -     |
| Instant         |  ✔   |    ✔    |       -       |       -        |       -       |     -     |     -      |     -     |
| ZonedDateTime   |  ✔   |    ✔    |       ✔       |       ✔        |       ✔       |     ✔     |     ✔      |     ✔     |
| OffsetDateTime  |  ✔   |    ✔    |       ✔       |       ✔        |       ✔       |     ✔     |     ✔      |     ✔     |
| LocalDateTime   |  -   |    -    |       -       |       -        |       ✔       |     ✔     |     -      |     ✔     |
| LocalDate       |  -   |    -    |       -       |       -        |       -       |     ✔     |     -      |     -     |
| OffsetTime      |  -   |    -    |       -       |       -        |       -       |     -     |     ✔      |     ✔     |
| LocalTime       |  -   |    -    |       -       |       -        |       -       |     -     |     -      |     ✔     |

| From\To        | String | Long (Epoch ms) | Long (Epoch s) |
|----------------|:------:|:---------------:|:--------------:|
| Date           |   ✔    |        ✔        |       ☑        |
| Instant        |   ✔    |        ✔        |       ☑        |
| ZonedDateTime  |   ✔    |        ✔        |       ☑        |
| OffsetDateTime |   ✔    |        ✔        |       ☑        |
| LocalDateTime  |   ✔    |        -        |       -        |
| LocalDate      |   ✔    |        -        |       -        |
| OffsetTime     |   ✔    |        -        |       -        |
| LocalTime      |   ✔    |        -        |       -        |

## Enum

TODO: document

* EnumToEnumConverter
* EnumToXConverter
* XToEnumConverter

## Iterables

TODO: document

* IterableToIterableConverter

## Maps

TODO: document

* MapToMapConverter

## Other

TODO: document

* ToAny
* SameType
