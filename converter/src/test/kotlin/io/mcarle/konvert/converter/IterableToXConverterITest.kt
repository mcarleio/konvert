package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.SourceToTargetTypeNamePair
import io.mcarle.konvert.converter.utils.VerificationData
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.paukov.combinatorics3.Generator


@OptIn(ExperimentalCompilerApi::class)
class IterableToXConverterITest : ConverterITest() {

    companion object {

        private fun createConverter(targetTypeName: String): TypeConverter {
            return when (targetTypeName) {
                ITERABLE -> IterableToIterableConverter()
                MUTABLEITERABLE -> IterableToMutableIterableConverter()
                COLLECTION -> IterableToCollectionConverter()
                MUTABLECOLLECTION -> IterableToMutableCollectionConverter()
                LIST -> IterableToListConverter()
                MUTABLELIST -> IterableToMutableListConverter()
                ARRAYLIST -> IterableToArrayListConverter()
                SET -> IterableToSetConverter()
                MUTABLESET -> IterableToMutableSetConverter()
                HASHSET -> IterableToHashSetConverter()
                LINKEDHASHSET -> IterableToLinkedHashSetConverter()

                else -> throw RuntimeException("Unknown iterable target type: $targetTypeName")
            }
        }

        @JvmStatic
        fun supportedIterableConverters() = IterableToXConverter.supported().map { arguments(it) }

        @JvmStatic
        fun cartesianProductOfTypes() = IterableToXConverter
            .supported()
            .flatMap { listOf(it, "$it?") }
            .let { Generator.cartesianProduct(it, it) }
            .removeSourceNullableAndTargetNotNull()
            .groupBy { it[1].removeSuffix("?") }
            .flatMap { entry ->
                fun typeNamePairs(sourceGenericTypeName: String, targetGenericTypeName: String): List<SourceToTargetTypeNamePair> {
                    return entry.value.map { (sourceIterableTypeName, targetIterableTypeName) ->
                        val source = sourceIterableTypeName.removeSuffix("?")
                        val target = targetIterableTypeName.removeSuffix("?")
                        SourceToTargetTypeNamePair(
                            "$source<$sourceGenericTypeName>${sourceIterableTypeName.commonSuffixWith("?")}",
                            "$target<$targetGenericTypeName>${targetIterableTypeName.commonSuffixWith("?")}",
                        )
                    }
                }
                listOf(
                    arguments(entry.key, typeNamePairs("String", "String")),
                    arguments(entry.key, typeNamePairs("String", "String?")),
                    arguments(entry.key, typeNamePairs("String?", "String?")),
                    arguments(entry.key, typeNamePairs("String", "Int")),
                    arguments(entry.key, typeNamePairs("String", "Int?")),
                    arguments(entry.key, typeNamePairs("MyString?", "MyInt?")), // special case: typealiases
                    arguments(entry.key, typeNamePairs("Collection<String>", "MutableCollection<String?>")),
                    arguments(entry.key, typeNamePairs("Collection<String>", "MutableCollection<String?>?")),
                    arguments(entry.key, typeNamePairs("Collection<String>?", "MutableCollection<String?>?")),
                    arguments(entry.key, typeNamePairs("Collection<MyString>", "MutableIterable<MyString?>")), // special case: typealiases
                    arguments(entry.key, typeNamePairs("Collection<String>", "MutableIterable<String?>?"))
                )
            }

    }

    @ParameterizedTest
    @MethodSource("cartesianProductOfTypes")
    fun convertersTest(converter: String, typeNamePairs: List<SourceToTargetTypeNamePair>) {
        executeTest(
            typeNamePairs,
            createConverter(converter),
            additionalCode = this.generateAdditionalCode(),
            additionalConverter = arrayOf(
                SameTypeConverter(),
                StringToIntConverter(),
                IterableToMutableCollectionConverter(),
                IterableToMutableIterableConverter(),
            )
        )
    }

    @ParameterizedTest
    @MethodSource("supportedIterableConverters")
    fun toAllIterables(targetTypeName: String) {
        executeTest(
            sourceTypeName = "$ITERABLE<String>",
            targetTypeName = "$targetTypeName<Int>",
            converter = createConverter(targetTypeName),
            additionalConverter = arrayOf(StringToIntConverter())
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
            val genericTypeName = sourceTypeName.substringAfter("<").removeSuffix(">").trim()
            val collectionValue: Any? = when {
                genericTypeName.startsWith("String") -> "888"
                genericTypeName.startsWith("MyString") -> "888"
                genericTypeName.startsWith("Int") -> 73
                genericTypeName.startsWith("Collection<String>") ||
                    genericTypeName.startsWith("$COLLECTION<String>") -> listOf("123")
                genericTypeName.startsWith("Collection<MyString>") ||
                    genericTypeName.startsWith("$COLLECTION<MyString>") -> listOf("123")
                else -> null
            }

            when {
                sourceTypeName.startsWith(ITERABLE) -> listOf(collectionValue).asIterable()
                sourceTypeName.startsWith(MUTABLEITERABLE) -> mutableSetOf(collectionValue)
                sourceTypeName.startsWith(COLLECTION) -> setOf(collectionValue)
                sourceTypeName.startsWith(MUTABLECOLLECTION) -> mutableListOf(collectionValue)
                sourceTypeName.startsWith(LIST) -> listOf(collectionValue)
                sourceTypeName.startsWith(MUTABLELIST) -> mutableListOf(collectionValue)
                sourceTypeName.startsWith(ARRAYLIST) -> ArrayList(listOf(collectionValue))
                sourceTypeName.startsWith(SET) -> setOf(collectionValue)
                sourceTypeName.startsWith(MUTABLESET) -> mutableSetOf(collectionValue)
                sourceTypeName.startsWith(HASHSET) -> HashSet(setOf(collectionValue))
                sourceTypeName.startsWith(LINKEDHASHSET) -> LinkedHashSet(setOf(collectionValue))
                else -> null
            }
        }

        val sourceInstance = verificationData.sourceKClass.constructors.first().call(*sourceValues.toTypedArray())

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEach { targetVariable ->
            val targetName = targetVariable.first
            assertDoesNotThrow {
                verificationData.targetKClass.members.first { it.name == targetName }.call(targetInstance) as Iterable<*>
            }
        }
    }

}
