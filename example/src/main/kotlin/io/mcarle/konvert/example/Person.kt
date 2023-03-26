package io.mcarle.konvert.example

import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.IntToUIntConverter
import io.mcarle.konvert.converter.LongToUIntConverter

@KonvertTo(
    value = PersonDto::class,
    mappings = [
        Mapping(source = "age", target = "numberOfYearsSinceBirth", enable = [LongToUIntConverter::class]),
    ]
)
@KonvertTo(value = PersonDao::class)
data class Person(
    val name: String,
    val address: Address,
    val age: Long
)
