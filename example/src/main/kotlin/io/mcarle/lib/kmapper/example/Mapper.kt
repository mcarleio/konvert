package io.mcarle.lib.kmapper.example

import io.mcarle.lib.kmapper.annotation.KMap
import io.mcarle.lib.kmapper.annotation.KMapper
import io.mcarle.lib.kmapper.annotation.KMapping


@KMapper
interface Mapper {
    @KMapping(
        mappings = [
            KMap(source = "streetName", target = "street"),
            KMap(source = "zipCode", target = "zip")
        ]
    )
    fun fromDto(dto: AddressDto): Address

    @KMapping(
        mappings = [
            KMap(source = "street", target = "streetName"),
            KMap(source = "zip", target = "zipCode")
        ]
    )
    fun toDto(address: Address): AddressDto

    @KMapping(mappings = [KMap(target = "age", expression = "42"),KMap(target = "age", expression = "42", constant = "", ignore = true)])
    fun fromDto(dto: PersonDto): Person

    @KMapping(mappings = [KMap(source = "age", target = "ageX")])
    fun toDto(person: Person): PersonDto
}