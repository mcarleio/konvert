package io.mcarle.kmap.example

import io.mcarle.kmap.api.KMappers

fun main() {
    val mapper = KMappers.get<Mapper>()

    val originalPersonDto = PersonDto(
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

    val person = originalPersonDto.mapToPerson()
    val person2 = mapper.fromDto(originalPersonDto)

    val dto = person.mapToPersonDto()
    val dto2 = mapper.toDto(person2)

    val dao = person.mapToPersonDao()
    val dao2 = person2.mapToPersonDao()

    println("Original Person DTO:  $originalPersonDto")
    println("Person:  $person")
    println("Person:  $person2")
    println("PersonDto: $dto")
    println("PersonDto2: $dto2")
    println("PersonDao: $dao")
    println("PersonDao2: $dao2")

    println("Equals? ${originalPersonDto == dto}")
    println("Equals? ${originalPersonDto == dto2}")

    println(FullName("Marcel Carlé").mapToLastName().lastName)
    println(Audi().mapToCar())
    println(Car2().mapToDefectCar())
    println(Car3(1234.00).mapToDefectCar2())
    println(A().mapToB())


}