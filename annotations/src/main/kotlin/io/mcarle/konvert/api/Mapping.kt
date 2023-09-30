package io.mcarle.konvert.api

/**
 * Describe how the value for [target] field should be determined. Rules are applied in following order:
 * * if [ignore] is set the target value is not mandatory, skip the field
 * * if [source] is set, map the source field to the target field
 * * if [constant] is set, set the target field to the constant value
 * * if [expression] is set, set the target field to the expression
 *
 * See also [validate] for not allowed configurations
 */
@Retention(AnnotationRetention.SOURCE)
annotation class Mapping(
    /**
     * The property name of the target class
     */
    val target: String,
    /**
     * The source property name of the source class.
     */
    val source: String = "",
    /**
     * The value provided here will be placed __as is__ into the generated code
     * (hint: if you want a string, you have to use escaped quotes!)
     *
     * ```kotlin
     * @KonvertTo(Car::class, mappings=[
     *      Mapping(target="brand", constant="Brand.AUDI")
     * ])
     * class Audi
     * class Car(val brand: Brand)
     * enum class Brand { AUDI }
     * ```
     *
     * __For expressions, use [expression], which guarantee you a constant name to access the source object__
     */
    val constant: String = "",
    /**
     * You can use the variable named __`it`__ to get the source object.
     * You have to make sure yourself, that you handle the nullability correct in your expression. __`it`__ is always not null.
     *
     * ```kotlin
     * @KonvertTo(LastName::class, mappings=[
     *      Mapping(target="lastName", expression="it.fullName?.split(\" \")?.last()")
     * ])
     * class FullName(val fullName: String?)
     * class LastName(val lastName: String?)
     * ```
     */
    val expression: String = "",
    /**
     * You normally need to provide a value for all declared properties on the target. In some cases, you can ignore setting a value.
     * The following scenarios may apply:
     * * Your target has a mutable property (with the same name as a field in the source), which you do not want to be modified
     *    ```kotlin
     *    @KonvertTo(DefectCar::class, mappings=[
     *         Mapping(target="price", ignore=true)
     *    ])
     *    class NewCar(val price: Double)
     *    class DefectCar {
     *      var price: Double? = null
     *    }
     *    ```
     * * Your source does not have the information for a nullable target
     *    ```kotlin
     *    @KonvertTo(DefectCar::class, mappings=[
     *         Mapping(target="repairCosts", ignore=true)
     *    ])
     *    class Car
     *    class DefectCar(val repairCosts: Double?)
     *    ```
     */
    val ignore: Boolean = false,
    /**
     * Some TypeConverters are not enabled by default (see [TypeConverter.enabledByDefault]).
     * With this setting, you can enable them for this specific mapping.
     * ```kotlin
     * @KonvertTo(PersonDto::class, mappings=[
     *      Mapping(target="age", enable=[STRING_TO_INT_CONVERTER])
     * ])
     * class Person(val age: String)
     * class PersonDto(val age: Int)
     * ```
     *
     * See in package [io.mcarle.konvert.api.converter] for a list of provided type converter names
     */
    val enable: Array<TypeConverterName> = []

) {
    companion object
}

typealias TypeConverterName = String

/**
 * Throws exceptions when no params (beside [Mapping.target]) are defined or an illegal parameter combination is defined.
 */
fun Mapping.validate() {
    fun noParam() = !ignore && source.isBlank() && constant.isBlank() && expression.isBlank() && enable.isEmpty()
    fun notAllowedParameterCombination(): List<String>? {
        val result = mutableListOf<String>()
        if (ignore) result += Mapping::ignore.name
        if (source.isNotBlank()) result += Mapping::source.name
        if (constant.isNotBlank()) result += Mapping::constant.name
        if (expression.isNotBlank()) result += Mapping::expression.name

        return if (result.size > 1) {
            result
        } else {
            null
        }
    }

    if (noParam()) throw NoParamDefinedException(target)
    notAllowedParameterCombination()?.let { throw NotAllowedParameterCombinationException(target, it) }
}

class NoParamDefinedException(target: String) : RuntimeException("Missing parameter for target=$target")

class NotAllowedParameterCombinationException(target: String, params: List<String>) :
    RuntimeException("Not allowed parameter combination for target=$target: $params")
