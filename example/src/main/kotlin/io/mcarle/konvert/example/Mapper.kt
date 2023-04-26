package io.mcarle.konvert.example

import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.converter.LONG_TO_UINT_CONVERTER
import io.mcarle.konvert.api.converter.STRING_TO_INT_CONVERTER
import io.mcarle.konvert.injector.spring.KComponent

@Konverter
@KComponent
interface Mapper {
    @Konvert(
        mappings = [
            Mapping(source = "streetName", target = "street"),
            Mapping(source = "zipCode", target = "zip")
        ]
    )
    fun fromDto(dto: AddressDto): Address

    @Konvert(
        mappings = [
            Mapping(source = "street", target = "streetName"),
            Mapping(source = "zip", target = "zipCode"),
            Mapping(source = "streetNumber", target = "streetNumber", enable = [STRING_TO_INT_CONVERTER])
        ]
    )
    fun toDto(address: Address): AddressDto

    @Konvert(
        mappings = [Mapping(source = "numberOfYearsSinceBirth", target = "age")]
    )
    fun fromDto(dto: PersonDto): Person

    @Konvert(mappings = [Mapping(source = "age", target = "numberOfYearsSinceBirth", enable = [LONG_TO_UINT_CONVERTER])])
    fun toDto(person: Person): PersonDto
}
