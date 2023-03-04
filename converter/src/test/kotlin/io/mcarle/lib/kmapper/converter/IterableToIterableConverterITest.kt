package io.mcarle.lib.kmapper.converter

import io.mcarle.lib.kmapper.converter.api.TypeConverter
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.paukov.combinatorics3.Generator
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class IterableToIterableConverterITest : ConverterITest() {

    companion object {

        @JvmStatic
        fun cartesianProductOfTypes() = listOf(
            "kotlin.collections.Iterable",
            "kotlin.collections.MutableIterable",
            "kotlin.collections.Collection",
            "kotlin.collections.MutableCollection",
            "kotlin.collections.List",
            "kotlin.collections.MutableList",
            "java.util.ArrayList",
            "kotlin.collections.Set",
            "kotlin.collections.MutableSet",
            "java.util.HashSet",
            "java.util.LinkedHashSet"
        )
            .flatMap { listOf(it, "$it?") }
            .let { Generator.cartesianProduct(it, it) }
            .removeSourceNullableAndTargetNotNull()
            .flatMap {
                listOf(
                    arguments(it[0], "String", it[1], "String"),
                    arguments(it[0], "String", it[1], "String?"),
                    arguments(it[0], "String?", it[1], "String?"),
                    arguments(it[0], "String", it[1], "Int"),
                    arguments(it[0], "String", it[1], "Int?"),
                    arguments(it[0], "String?", it[1], "Int?"),
                    arguments(it[0], "Collection<String>", it[1], "MutableCollection<String?>"),
                    arguments(it[0], "Collection<String>", it[1], "MutableCollection<String?>?"),
                    arguments(it[0], "Collection<String>?", it[1], "MutableCollection<String?>?"),
                    arguments(it[0], "Collection<String>", it[1], "MutableIterable<String?>"),
                    arguments(it[0], "Collection<String>", it[1], "MutableIterable<String?>?"),
                    arguments(it[0], "Collection<String>?", it[1], "MutableIterable<String?>?"),
                )
            }

    }

    @ParameterizedTest
    @MethodSource("cartesianProductOfTypes")
    fun converterTest(sourceTypeName: String, sourceGenericTypeName: String, targetTypeName: String, targetGenericTypeName: String) {
        val sourceTypeNameWithoutSuffix = sourceTypeName.removeSuffix("?")
        val targetTypeNameWithoutSuffix = targetTypeName.removeSuffix("?")
        super.converterTest(
            IterableToIterableConverter(),
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
        val genericTypeName = sourceTypeName.substringAfter("<").removeSuffix(">").trim()
        val collectionValue: Any? = when {
            genericTypeName.startsWith("String") -> "888"
            genericTypeName.startsWith("Int") -> 73
            genericTypeName.startsWith("Collection<String>") -> listOf("123")
            else -> null
        }

        val sourceInstance = sourceKClass.constructors.first().call(
            when {
                sourceTypeName.startsWith("kotlin.collections.Iterable") -> listOf(collectionValue).asIterable()
                sourceTypeName.startsWith("kotlin.collections.MutableIterable") -> mutableSetOf(collectionValue)
                sourceTypeName.startsWith("kotlin.collections.Collection") -> setOf(collectionValue)
                sourceTypeName.startsWith("kotlin.collections.MutableCollection") -> mutableListOf(collectionValue)
                sourceTypeName.startsWith("kotlin.collections.List") -> listOf(collectionValue)
                sourceTypeName.startsWith("kotlin.collections.MutableList") -> mutableListOf(collectionValue)
                sourceTypeName.startsWith("java.util.ArrayList") -> ArrayList(listOf(collectionValue))
                sourceTypeName.startsWith("kotlin.collections.Set") -> setOf(collectionValue)
                sourceTypeName.startsWith("kotlin.collections.MutableSet") -> mutableSetOf(collectionValue)
                sourceTypeName.startsWith("java.util.HashSet") -> HashSet(setOf(collectionValue))
                sourceTypeName.startsWith("java.util.LinkedHashSet") -> LinkedHashSet(setOf(collectionValue))
                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance) as Iterable<*>
        }
    }

}

