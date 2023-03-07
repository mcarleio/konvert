package io.mcarle.lib.kmapper.example

import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.api.annotation.KMapper
import io.mcarle.lib.kmapper.api.annotation.KMapping
import io.mcarle.lib.kmapper.converter.IntToULongConverter
import io.mcarle.lib.kmapper.converter.StringToIntConverter


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
            KMap(source = "zip", target = "zipCode"),
            KMap(source = "streetNumber", target = "streetNumber", enable = [StringToIntConverter::class])
        ]
    )
    fun toDto(address: Address): AddressDto

//    @KMapping(mappings = [KMap(target = "age", expression = "42"), KMap(target = "age", expression = "42", constant = "", ignore = true)])
//    fun fromDto(dto: PersonDto): Person
    fun fromDto(dto: PersonDto): Person = Person(
    name = dto.name,
    address =
    io.mcarle.lib.kmapper.api.KMappers.get<io.mcarle.lib.kmapper.example.Mapper>().fromDto(dto =
    dto.address),
    age = dto.let { 42 }
)

    @KMapping(mappings = [KMap(source = "age", target = "ageX", enable = [IntToULongConverter::class])])
    fun toDto(person: Person): PersonDto
}