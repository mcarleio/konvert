package io.mcarle.kmap.example

import io.mcarle.kmap.api.annotation.KMap
import io.mcarle.kmap.api.annotation.KMapTo

@KMapTo(
    value = Person::class,
    mappings = [
        KMap(target = "age", expression = "42")
    ]
)
data class PersonDto(
    val name: String,
    val address: AddressDto,
    val ageX: ULong
)