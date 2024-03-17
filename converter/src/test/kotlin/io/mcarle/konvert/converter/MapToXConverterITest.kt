package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.SourceToTargetTypeNamePair
import io.mcarle.konvert.converter.utils.VerificationData
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.paukov.combinatorics3.Generator

@OptIn(ExperimentalCompilerApi::class)
class MapToXConverterITest : ConverterITest() {

    companion object {

        private fun createConverter(targetTypeName: String): TypeConverter {
            return when (targetTypeName) {
                MAP -> MapToMapConverter()
                MUTABLEMAP -> MapToMutableMapConverter()
                HASHMAP,
                JAVA_HASHMAP -> MapToHashMapConverter()
                LINKEDHASHMAP,
                JAVA_LINKEDHASHMAP -> MapToLinkedHashMapConverter()
                PERISTENT_MAP -> MapToPersistentMapConverter()
                IMMUTABLE_MAP -> MapToImmutableMapConverter()
                else -> throw RuntimeException("Unknown map target type: $targetTypeName")
            }
        }

        @JvmStatic
        fun supportedMapConverters() = MapToXConverter.supported().map { arguments(it) }

        @JvmStatic
        fun cartesianProductOfTypes() = MapToXConverter
            .supported()
            .flatMap { listOf(it, "$it?") }
            .let { Generator.cartesianProduct(it, it) }
            .removeSourceNullableAndTargetNotNull()
            .groupBy { it[1].removeSuffix("?") }
            .flatMap { entry ->
                fun typeNamePairs(
                    sourceKeyGenericTypeName: String, sourceValueGenericTypeName: String,
                    targetKeyGenericTypeName: String, targetValueGenericTypeName: String
                ): List<SourceToTargetTypeNamePair> {
                    return entry.value.map { (sourceIterableTypeName, targetIterableTypeName) ->
                        val source = sourceIterableTypeName.removeSuffix("?")
                        val target = targetIterableTypeName.removeSuffix("?")
                        SourceToTargetTypeNamePair(
                            "$source<$sourceKeyGenericTypeName, $sourceValueGenericTypeName>${sourceIterableTypeName.commonSuffixWith("?")}",
                            "$target<$targetKeyGenericTypeName, $targetValueGenericTypeName>${targetIterableTypeName.commonSuffixWith("?")}",
                        )
                    }
                }
                listOf(
                    arguments(entry.key, typeNamePairs("String", "String", "String", "String")),
                    arguments(entry.key, typeNamePairs("String", "String", "String?", "String")),
                    arguments(entry.key, typeNamePairs("String", "String", "String", "String?")),
                    arguments(entry.key, typeNamePairs("String", "String", "String?", "String?")),
                    arguments(entry.key, typeNamePairs("String", "String?", "String", "String?")),
                    arguments(entry.key, typeNamePairs("String", "String?", "String?", "String?")),
                    arguments(entry.key, typeNamePairs("String?", "String", "String?", "String")),
                    arguments(entry.key, typeNamePairs("MyString?", "MyString", "MyString?", "MyString?")),
                    arguments(entry.key, typeNamePairs("String?", "String?", "String?", "String?")),
                    arguments(entry.key, typeNamePairs("String", "String", "Int", "Int")),
                    arguments(entry.key, typeNamePairs("String", "String", "Int?", "Int")),
                    arguments(entry.key, typeNamePairs("String", "String", "Int", "Int?")),
                    arguments(entry.key, typeNamePairs("String", "String", "Int?", "Int?")),
                    arguments(entry.key, typeNamePairs("String", "String?", "Int", "Int?")),
                    arguments(entry.key, typeNamePairs("String", "String?", "Int?", "Int?")),
                    arguments(entry.key, typeNamePairs("MyString?", "MyString", "MyInt?", "MyInt")),
                    arguments(entry.key, typeNamePairs("String?", "String", "Int?", "Int?")),
                    arguments(entry.key, typeNamePairs("String?", "String?", "Int?", "Int?")),
                )
            }
    }

    @ParameterizedTest
    @MethodSource("cartesianProductOfTypes")
    fun convertersTest(converter: String, typeNamePairs: List<SourceToTargetTypeNamePair>) {
        executeTest(
            typeNamePairs = typeNamePairs,
            converter = createConverter(converter),
            additionalConverter = this.additionalConverter(),
            additionalCode = this.generateAdditionalCode()
        )
    }

    @ParameterizedTest
    @MethodSource("supportedMapConverters")
    fun toAllMaps(targetTypeName: String) {
        executeTest(
            sourceTypeName = "$MAP<String, String>",
            targetTypeName = "$targetTypeName<Int, Int>",
            converter = createConverter(targetTypeName),
            additionalConverter = this.additionalConverter()
        )
    }

    private fun additionalConverter(): Array<TypeConverter> {
        return arrayOf(
            SameTypeConverter(),
            StringToIntConverter()
        )
    }

    private fun generateAdditionalCode(): List<SourceFile> = listOf(
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

    override fun verify(verificationData: VerificationData) {
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceTypeName = sourceVariable.second
            val genericKeyTypeName = sourceTypeName.substringAfter("<").split(",").first().trim()
            val key: Any? = when {
                genericKeyTypeName.startsWith("String") -> "888"
                genericKeyTypeName.startsWith("MyString") -> "888"
                genericKeyTypeName.startsWith("Int") -> 73
                genericKeyTypeName.startsWith("MyInt") -> 73
                else -> null
            }
            val genericValueTypeName = sourceTypeName.substringBefore(">").split(",").last().trim()
            val value: Any? = when {
                genericValueTypeName.startsWith("String") -> "888"
                genericValueTypeName.startsWith("MyString") -> "888"
                genericValueTypeName.startsWith("Int") -> 73
                genericValueTypeName.startsWith("MyInt") -> 73
                else -> null
            }

            when {
                sourceTypeName.startsWith(MAP) -> mapOf(key to value)
                sourceTypeName.startsWith(MUTABLEMAP) -> mutableMapOf(key to value)
                sourceTypeName.startsWith(JAVA_HASHMAP) -> java.util.HashMap(mapOf(key to value))
                sourceTypeName.startsWith(JAVA_LINKEDHASHMAP) -> java.util.LinkedHashMap(mapOf(key to value))
                sourceTypeName.startsWith(HASHMAP) -> HashMap(mapOf(key to value))
                sourceTypeName.startsWith(LINKEDHASHMAP) -> LinkedHashMap(mapOf(key to value))
                sourceTypeName.startsWith(PERISTENT_MAP) -> persistentMapOf(key to value)
                sourceTypeName.startsWith(IMMUTABLE_MAP) -> persistentMapOf(key to value).toImmutableMap()
                else -> null
            }
        }

        val sourceInstance = verificationData.sourceKClass.constructors.first().call(*sourceValues.toTypedArray())

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEach { targetVariable ->
            val targetName = targetVariable.first
            assertDoesNotThrow {
                verificationData.targetKClass.members.first { it.name == targetName }.call(targetInstance) as Map<*, *>
            }
        }
    }

}

