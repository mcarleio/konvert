package io.mcarle.lib.kmapper.example

import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.api.annotation.KMapTo
import io.mcarle.lib.kmapper.converter.IntToULongConverter

@KMapTo(
    value = PersonDto::class,
    mappings = [
        KMap(target = "name"),
        KMap(source = "age", target = "ageX", enable = [IntToULongConverter::class]),
    ]
)
data class Person(
    val name: String,
    val address: Address,
    val age: Int
)
