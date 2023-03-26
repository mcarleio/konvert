package io.mcarle.kmap.example

import io.mcarle.kmap.api.annotation.KMap
import io.mcarle.kmap.api.annotation.KMapTo

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
