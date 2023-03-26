package io.mcarle.kmap.example

import io.mcarle.kmap.api.annotation.KMap
import io.mcarle.kmap.api.annotation.KMapTo
import io.mcarle.kmap.converter.IntToULongConverter

@KMapTo(
    value = PersonDto::class,
    mappings = [
        KMap(target = "name"),
        KMap(source = "age", target = "ageX", enable = [IntToULongConverter::class]),
    ]
)
@KMapTo(value = PersonDao::class)
data class Person(
    val name: String,
    val address: Address,
    val age: Int
)
