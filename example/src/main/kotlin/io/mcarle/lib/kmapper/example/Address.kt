package io.mcarle.lib.kmapper.example

import io.mcarle.lib.kmapper.annotation.KMap
import io.mcarle.lib.kmapper.annotation.KMapTo

@KMapTo(
    AddressDto::class,
    mappings = [
        KMap(source = "street", target = "streetName"),
        KMap(source = "zip", target = "zipCode")
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
