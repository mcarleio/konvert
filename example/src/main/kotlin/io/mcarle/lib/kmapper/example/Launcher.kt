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
        ),
        1u
    )

    val person = dto.mapToPerson()
    val person2 = mapper.fromDto(dto)

    val dto22 = mapper.toDto(person2)
    val dto2 = person.mapToPersonDto()

    println("PersonDto:  $dto")
    println("Person:  $person")
    println("Person:  $person2")
    println("PersonDto2: $dto2")
    println("PersonDto2: $dto22")

    println("Equals? ${dto == dto2}")
    println("Equals? ${dto == dto22}")

    println(FullName("Marcel Carlé").mapToLastName().lastName)
    println(Audi().mapToCar())
    println(Car2().mapToDefectCar())
    println(Car3(1234.00).mapToDefectCar2())
    println(A().mapToB())


}