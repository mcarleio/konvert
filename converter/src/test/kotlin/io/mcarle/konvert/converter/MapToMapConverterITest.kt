package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
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
                    arguments(it[0], "MyString?, MyString", it[1], "MyString?, MyString?"),
                    arguments(it[0], "String?, String?", it[1], "String?, String?"),
                    arguments(it[0], "String, String", it[1], "Int, Int"),
                    arguments(it[0], "String, String", it[1], "Int?, Int"),
                    arguments(it[0], "String, String", it[1], "Int, Int?"),
                    arguments(it[0], "String, String", it[1], "Int?, Int?"),
                    arguments(it[0], "String, String?", it[1], "Int, Int?"),
                    arguments(it[0], "String, String?", it[1], "Int?, Int?"),
                    arguments(it[0], "MyString?, MyString", it[1], "MyInt?, MyInt"),
                    arguments(it[0], "String?, String", it[1], "Int?, Int?"),
                    arguments(it[0], "String?, String?", it[1], "Int?, Int?"),
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
            MapToMapConverter(),
            "$sourceTypeNameWithoutSuffix<$sourceGenericTypeName>${sourceTypeName.commonSuffixWith("?")}",
            "$targetTypeNameWithoutSuffix<$targetGenericTypeName>${targetTypeName.commonSuffixWith("?")}"
        )
    }

    @Test
    fun simple() {
        super.converterTest(
            MapToMapConverter(),
            "kotlin.collections.Map<String, Int>",
            "java.util.LinkedHashMap<Int, Int?>"
        )
    }

    override fun additionalConverter(): Array<TypeConverter> {
        return arrayOf(
            SameTypeConverter(),
            StringToIntConverter()
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


    override fun verifyMapper(
        converter: TypeConverter,
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
        val keyTypeName = sourceTypeName.substringAfter("<").split(",").first().trim()
        val key = when {
            keyTypeName.startsWith("MyString") -> "888"
            keyTypeName.startsWith("String") -> "888"
            keyTypeName.startsWith("MyInt") -> 73
            keyTypeName.startsWith("Int") -> 73
            else -> null
        }
        val valueTypeName = sourceTypeName.substringBefore(">").split(",").last().trim()
        val value = when {
            valueTypeName.startsWith("MyString") -> "37173"
            valueTypeName.startsWith("String") -> "37173"
            valueTypeName.startsWith("MyInt") -> 42
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

