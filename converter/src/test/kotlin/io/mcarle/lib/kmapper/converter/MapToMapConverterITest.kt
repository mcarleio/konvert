package io.mcarle.lib.kmapper.converter

import io.mcarle.lib.kmapper.converter.api.TypeConverter
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.paukov.combinatorics3.Generator
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class MapToMapConverterITest : ConverterITest() {

    companion object {

        @JvmStatic
        fun cartesianProductOfTypes() = listOf(
            "kotlin.collections.Map",
            "kotlin.collections.MutableMap",
            "java.util.HashMap",
            "java.util.LinkedHashMap"
        )
            .flatMap { listOf(it, "$it?") }
            .let { Generator.cartesianProduct(it, it) }
            .removeSourceNullableAndTargetNotNull()
            .flatMap {
                listOf(
                    arguments(it[0], "String, String", it[1], "String, String"),
                    arguments(it[0], "String, String", it[1], "String?, String"),
                    arguments(it[0], "String, String", it[1], "String, String?"),
                    arguments(it[0], "String, String", it[1], "String?, String?"),
                    arguments(it[0], "String, String?", it[1], "String, String?"),
                    arguments(it[0], "String, String?", it[1], "String?, String?"),
                    arguments(it[0], "String?, String", it[1], "String?, String"),
                    arguments(it[0], "String?, String", it[1], "String?, String?"),
                    arguments(it[0], "String?, String?", it[1], "String?, String?"),
                    arguments(it[0], "String, String", it[1], "Int, Int"),
                    arguments(it[0], "String, String", it[1], "Int?, Int"),
                    arguments(it[0], "String, String", it[1], "Int, Int?"),
                    arguments(it[0], "String, String", it[1], "Int?, Int?"),
                    arguments(it[0], "String, String?", it[1], "Int, Int?"),
                    arguments(it[0], "String, String?", it[1], "Int?, Int?"),
                    arguments(it[0], "String?, String", it[1], "Int?, Int"),
                    arguments(it[0], "String?, String", it[1], "Int?, Int?"),
                    arguments(it[0], "String?, String?", it[1], "Int?, Int?"),
                )
            }

    }

    @ParameterizedTest
    @MethodSource("cartesianProductOfTypes")
    fun converterTest(sourceTypeName: String, sourceGenericTypeName: String, targetTypeName: String, targetGenericTypeName: String) {
        val sourceTypeNameWithoutSuffix = sourceTypeName.removeSuffix("?")
        val targetTypeNameWithoutSuffix = targetTypeName.removeSuffix("?")

        super.converterTest(
            MapToMapConverter(),
            "$sourceTypeNameWithoutSuffix<$sourceGenericTypeName>${sourceTypeName.commonSuffixWith("?")}",
            "$targetTypeNameWithoutSuffix<$targetGenericTypeName>${targetTypeName.commonSuffixWith("?")}"
        )
    }

    override fun additionalConverter(): Array<TypeConverter> {
        return arrayOf(
            SameTypeConverter(),
            StringToIntConverter()
        )
    }

    override fun verifyMapper(
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
        val keyTypeName = sourceTypeName.substringAfter("<").split(",").first().trim()
        val key = when {
            keyTypeName.startsWith("String") -> "888"
            keyTypeName.startsWith("Int") -> 73
            else -> null
        }
        val valueTypeName = sourceTypeName.substringBefore(">").split(",").last().trim()
        val value = when {
            valueTypeName.startsWith("String") -> "37173"
            valueTypeName.startsWith("Int") -> 42
            else -> null
        }


        val sourceInstance = sourceKClass.constructors.first().call(
            when {
                sourceTypeName.startsWith("kotlin.collections.Map") -> mapOf(key to value)
                sourceTypeName.startsWith("kotlin.collections.MutableMap") -> mutableMapOf(key to value)
                sourceTypeName.startsWith("java.util.HashMap") -> HashMap(mapOf(key to value))
                sourceTypeName.startsWith("java.util.LinkedHashMap") -> LinkedHashMap(mapOf(key to value))
                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance) as Map<*, *>
        }
    }

}

