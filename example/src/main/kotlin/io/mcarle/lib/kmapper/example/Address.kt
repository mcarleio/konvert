package io.mcarle.lib.kmapper.example

import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.api.annotation.KMapTo
import io.mcarle.lib.kmapper.converter.StringToIntConverter

@KMapTo(
    AddressDto::class,
    mappings = [
        KMap(source = "street", target = "streetName"),
        KMap(source = "zip", target = "zipCode"),
        KMap(source = "streetNumber", target = "streetNumber", enable = [StringToIntConverter::class]),
    ],
    priority = 1
)
data class Address(
    val street: String,
    val streetNumber: String,
    val zip: String,
    val city: String,
    val country: String
)
