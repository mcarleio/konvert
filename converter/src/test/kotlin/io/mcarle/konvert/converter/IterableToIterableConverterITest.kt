package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.api.TypeConverter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
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
            "java.util.LinkedHashSet",
            "kotlinx.collections.immutable.ImmutableList",
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
                    arguments(it[0], "MyString?", it[1], "MyInt?"), // special case: typealiases
                    arguments(it[0], "Collection<String>", it[1], "MutableCollection<String?>"),
                    arguments(it[0], "Collection<String>", it[1], "MutableCollection<String?>?"),
                    arguments(it[0], "Collection<String>?", it[1], "MutableCollection<String?>?"),
                    arguments(it[0], "Collection<MyString>", it[1], "MutableIterable<MyString?>"), // special case: typealias
                    arguments(it[0], "Collection<String>", it[1], "MutableIterable<String?>?"),
                    arguments(it[0], "Collection<String>?", it[1], "MutableIterable<String?>?"),
                    arguments(it[0], "Collection<MyString>", it[1], "ImmutableList<MyString?>"), // special case: typealias
                    arguments(it[0], "Collection<String>", it[1], "ImmutableList<String?>?"),
                    arguments(it[0], "Collection<String>?", it[1], "ImmutableList<String?>?"),
                )
            }

    }

    @Tag("detailed")
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

    @Test
    fun simple() {
        super.converterTest(
            IterableToIterableConverter(),
            "kotlin.collections.List<String>",
            "kotlin.collections.MutableSet<Int>"
        )
    }

    override fun generateAdditionalCode(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            name = "MyTypealiases.kt",
            contents =
            """
typealias MyString = String
typealias ReallyMyInt = Int
typealias MyInt = ReallyMyInt
            """.trimIndent()
        )
    )


    override fun additionalConverter(): Array<TypeConverter> {
        return arrayOf(
            SameTypeConverter(),
            StringToIntConverter()
        )
    }

    override fun verifyMapper(
        converter: TypeConverter,
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
            genericTypeName.startsWith("MyString") -> "888"
            genericTypeName.startsWith("Int") -> 73
            genericTypeName.startsWith("Collection<String>") -> listOf("123")
            genericTypeName.startsWith("Collection<MyString>") -> listOf("123")
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
                sourceTypeName.startsWith("kotlinx.collections.immutable.ImmutableList") -> persistentListOf(collectionValue)
                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance) as Iterable<*>
        }
    }

}
