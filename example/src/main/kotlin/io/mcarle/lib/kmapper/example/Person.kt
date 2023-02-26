package io.mcarle.lib.kmapper.example

import io.mcarle.lib.kmapper.annotation.KMap
import io.mcarle.lib.kmapper.annotation.KMapTo

@KMapTo(
    value = PersonDto::class,
    mappings = [
        KMap(target = "name"),
        KMap(source = "age", target = "ageX"),
    ]
)
data class Person(
    val name: String,
    val address: Address,
    val age: Int
)
