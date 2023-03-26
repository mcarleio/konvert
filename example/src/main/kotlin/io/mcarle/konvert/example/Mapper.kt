package io.mcarle.konvert.example

import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.IntToUIntConverter
import io.mcarle.konvert.converter.LongToUIntConverter
import io.mcarle.konvert.converter.StringToIntConverter

@Konverter
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
            Mapping(source = "streetNumber", target = "streetNumber", enable = [StringToIntConverter::class])
        ]
    )
    fun toDto(address: Address): AddressDto

    @Konvert(
        mappings = [Mapping(source = "numberOfYearsSinceBirth", target = "age")]
    )
    fun fromDto(dto: PersonDto): Person

    @Konvert(mappings = [Mapping(source = "age", target = "numberOfYearsSinceBirth", enable = [LongToUIntConverter::class])])
    fun toDto(person: Person): PersonDto
}
