package io.mcarle.lib.kmapper.example

import io.mcarle.lib.kmapper.annotation.KMappers

fun main() {
    val mapper = KMappers.get<Mapper>()

    val dto = PersonDto(
        "Hans",
        AddressDto(
            "Some Street",
            123,
            "23432",
            "Example",
            "Somewhere"
        )
    )

    val person = mapper.fromDto(dto)

    val dto2 = mapper.toDto(person)

    println("PersonDto:  $dto")
    println("Person:  $person")
    println("PersonDto2: $dto2")

    println("Equals? ${dto == dto2}")


}