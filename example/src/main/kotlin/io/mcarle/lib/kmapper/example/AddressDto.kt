package io.mcarle.lib.kmapper.example

import io.mcarle.lib.kmapper.annotation.KMap
import io.mcarle.lib.kmapper.annotation.KMapTo

@KMapTo(
    Address::class,
    mappings = [
        KMap(source = "streetName", target = "street"),
        KMap(source = "zipCode", target = "zip")
    ]
)
data class AddressDto(
    val streetName: String,
    val streetNumber: Int,
    val zipCode: String,
    val city: String,
    val country: String
)
