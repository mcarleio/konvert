package io.mcarle.konvert.example

import io.mcarle.konvert.api.Konverter

data class ClassA(val text: String)
data class ClassB(val text: String)
class ListA(val list: List<ClassA>)
class ListB(val list: List<ClassB>)
class MapA(val map: Map<ClassA, ListA>)
class MapB(val map: Map<ClassB, ListB>)

@Konverter
interface ListMapper {
    fun classAToB(a: ClassA): ClassB
    fun listAToB(a: ListA): ListB
    fun mapAToB(a: MapA): MapB
}
