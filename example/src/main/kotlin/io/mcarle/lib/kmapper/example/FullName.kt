package io.mcarle.lib.kmapper.example

import io.mcarle.lib.kmapper.api.annotation.KMap
import io.mcarle.lib.kmapper.api.annotation.KMapTo

@KMapTo(
    LastName::class, mappings = [
        KMap(target = "lastName", expression = "it.fullName?.split(\" \")?.last()")
    ]
)
data class FullName(val fullName: String?)
data class LastName(val lastName: String?)

@KMapTo(
    Car::class, mappings = [
        KMap(target = "brand", constant = "Brand.AUDI"),
        KMap(target = "price", ignore = true),
    ]
)
class Audi()
data class Car(val brand: Brand, val price: Double = 123.0) {
    //    var price: Double = 123.1
    override fun toString(): String {
        return "Car(brand=$brand, price=$price)"
    }

}

enum class Brand {
    AUDI
}


@KMapTo(
    DefectCar::class, mappings = [
        KMap(target = "repairCosts", ignore = true)
    ]
)
class Car2
data class DefectCar(val repairCosts: Double?)

@KMapTo(
    DefectCar2::class, mappings = [
        KMap(target = "price", ignore = true, constant = "123.3"),
        KMap(target = "price", ignore = true),
        KMap(target = "price", expression = "asd"),
        KMap(target = "price", constant = "asd"),
        KMap(target = "price", constant = "asd", expression = "ccc"),
    ]
)
data class Car3(val price: Double)
class DefectCar2 {
    var price: Double? = null
}

@KMapTo(B::class)
class A
class B