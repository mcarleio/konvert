package io.mcarle.lib.kmapper.annotation

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
annotation class KMap(
    val source: String = "",
    val target: String,
    /**
     * The value provided here will be placed __as is__ into the generated code
     * (hint: if you want a string, you have to use escaped quotes!)
     *
     * ```kotlin
     * @KMapTo(Car::class, mappings=[
     *      KMap(target="brand", constant="Brand.AUDI")
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
     * @KMapTo(LastName::class, mappings=[
     *      KMap(target="lastName", expression="it.fullName?.split(\" \")?.last()")
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
     *    @KMapTo(DefectCar::class, mappings=[
     *         KMap(target="price", ignore=true)
     *    ])
     *    class NewCar(val price: Double)
     *    class DefectCar {
     *      var price: Double? = null
     *    }
     *    ```
     * * Your source does not have the information for a target with default value
     *    ```kotlin
     *    @KMapTo(CarToRepair::class, mappings=[
     *         KMap(target="defect", ignore=true)
     *    ])
     *    class Car
     *    class CarToRepair(val defect: Boolean = true)
     *    ```
     * * Your source does not have the information for a nullable target
     *    ```kotlin
     *    @KMapTo(DefectCar::class, mappings=[
     *         KMap(target="repairCosts", ignore=true)
     *    ])
     *    class Car
     *    class DefectCar(val repairCosts: Double?)
     *    ```
     */
    val ignore: Boolean = false
)

/**
 * Throws exceptions when no params (besides [KMap.target]) are defined or more than one param (besides [KMap.target]) is defined
 */
fun KMap.validate(logger: ((String, Boolean) -> Unit)? = null) {
    fun noParam() = !ignore && source.isBlank() && constant.isBlank() && expression.isBlank()
    fun moreThanOneParam(): List<String>? {
        val result = mutableListOf<String>()
        if (ignore) result += KMap::ignore.name
        if (source.isNotBlank()) result += KMap::source.name
        if (constant.isNotBlank()) result += KMap::constant.name
        if (expression.isNotBlank()) result += KMap::expression.name

        return if (result.size > 1) {
            result
        } else {
            null
        }
    }

    if (noParam()) {
        logger?.invoke("Missing parameter for target=$target", true)
    }
    moreThanOneParam()?.let {
        logger?.invoke("Only one parameter of $it allowed [target=$target]", false)
    }
}