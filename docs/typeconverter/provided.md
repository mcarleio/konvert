---
layout: default
parent: TypeConverter
title: Provided with Konvert
---

{% comment %} @formatter:off {% endcomment %}
# Provided with Konvert
{: .no_toc }

`Konvert` already provides a lot of default `TypeConverter`:
{: .fs-6.fw-300 }

- TOC
{:toc}
{% comment %} @formatter:on {% endcomment %}

{: .info }
> The following tables share this legend:
>
> * `✔` = a `TypeConverter` exists and is enabled by default
> * `☑` = a `TypeConverter` exists but is not enabled by default
> * empty = no `TypeConverter` existing

---

## Basic Types

{% include_relative gen/basic.md %}

## Temporal Types

{% include_relative gen/to_temporal.md %}

{% include_relative gen/from_temporal.md %}

## Enum

{% include_relative gen/to_enum.md %}

{% include_relative gen/from_enum.md %}

The `EnumToEnumConverter` will fail, if the target enum does not provide all possible values as the source enum.

## Iterables

{% include_relative gen/iterable.md %}

The values inside are also mapped, if a `TypeConverter` for them is enabled.

## Maps

{% include_relative gen/map.md %}

The key and value type parameters are also mapped, if a `TypeConverter` for both is enabled.

## Other

### ToAnyConverter

Special and simple case: target is the `Any` type from which all types inherit.

|   From\To    | kotlin.Any |
|:------------:|:----------:|
| **any type** |     ✔      |
{: .fixed-first-column-120 }

### SameTypeConverter

Special and simple case: source and target are exactly the same type.

|  From\To   | &lt;T> |
|:----------:|:------:|
| **&lt;T>** |   ✔    |
{: .fixed-first-column-120 }
