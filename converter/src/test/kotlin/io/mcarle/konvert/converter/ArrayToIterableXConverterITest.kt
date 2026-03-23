package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource


@OptIn(ExperimentalCompilerApi::class)
class ArrayToIterableXConverterITest : ConverterITest() {

    companion object {

        private fun createConverter(targetTypeName: String): TypeConverter {
            return when (targetTypeName) {
                ITERABLE -> ArrayToIterableConverter()
                MUTABLEITERABLE -> ArrayToMutableIterableConverter()
                COLLECTION -> ArrayToCollectionConverter()
                MUTABLECOLLECTION -> ArrayToMutableCollectionConverter()
                LIST -> ArrayToListConverter()
                MUTABLELIST -> ArrayToMutableListConverter()
                ARRAYLIST -> ArrayToArrayListConverter()
                SET -> ArrayToSetConverter()
                MUTABLESET -> ArrayToMutableSetConverter()
                HASHSET -> ArrayToHashSetConverter()
                LINKEDHASHSET -> ArrayToLinkedHashSetConverter()
                IMMUTABLE_COLLECTION -> ArrayToImmutableCollectionConverter()
                IMMUTABLE_LIST -> ArrayToImmutableListConverter()
                IMMUTABLE_SET -> ArrayToImmutableSetConverter()
                PERSISTENT_COLLECTION -> ArrayToPersistentCollectionConverter()
                PERSISTENT_LIST -> ArrayToPersistentListConverter()
                PERSISTENT_SET -> ArrayToPersistentSetConverter()
                else -> throw RuntimeException("Unknown iterable target type: $targetTypeName")
            }
        }

        @JvmStatic
        fun supportedArrayToIterableConverters() = ArrayToIterableXConverter.supported().map { arguments(it) }

    }

    @ParameterizedTest
    @MethodSource("supportedArrayToIterableConverters")
    fun supportedIterablesTest(targetTypeName: String) {
        executeTest(
            sourceTypeName = "Array<String>",
            targetTypeName = "$targetTypeName<Int>",
            converter = createConverter(targetTypeName),
            additionalConverter = arrayOf(StringToIntConverter())
        )
    }

    override fun verify(verificationData: VerificationData) {
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceTypeName = sourceVariable.second
            val genericTypeName = sourceTypeName.substringAfter("<").removeSuffix(">").trim()
            when {
                genericTypeName.startsWith("String") -> arrayOf("888")
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

    @Test
    fun variance() {
        val (compilation) = compileWith(
            enabledConverters = listOf(ArrayToListConverter(), SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetOutVarianceClass::class)
@KonvertTo(TargetNoVarianceClass::class)
class SourceClass(val property: Array<out Int>)
class TargetOutVarianceClass(val property: List<out Int>)
class TargetNoVarianceClass(val property: List<Int>)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")

        assertSourceEquals(
            """

public fun SourceClass.toTargetOutVarianceClass(): TargetOutVarianceClass = TargetOutVarianceClass(
  property = property.map { it }
)

public fun SourceClass.toTargetNoVarianceClass(): TargetNoVarianceClass = TargetNoVarianceClass(
  property = property.map { it }
)
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}
