package io.mcarle.konvert.example

import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.converter.StringToIntConverter

@KonvertTo(
    AddressDto::class,
    mappings = [
        Mapping(source = "street", target = "streetName"),
        Mapping(source = "zip", target = "zipCode"),
        Mapping(source = "streetNumber", target = "streetNumber", enable = [StringToIntConverter::class]),
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

@KonvertTo(
    Address::class,
    mappings = [
        Mapping(source = "streetName", target = "street"),
        Mapping(source = "zipCode", target = "zip")
    ]
)
data class AddressDto(
    val streetName: String,
    val streetNumber: Int,
    val zipCode: String,
    val city: String,
    val country: String
)
