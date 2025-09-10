package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class XToValueClassConverterITest : ConverterITest() {


    companion object {

        @JvmStatic
        fun sourceAndTargets(): List<Arguments> = listOf(
            "String",
            "Int",
        ).cartesianProductWithNullableCombinations(
            "SimpleValueClass",
            "ValueClassWithNullable",
            "ValueClassWithAdditionalProperties"
        )

    }

    @ParameterizedTest
    @MethodSource("sourceAndTargets")
    fun converterTest(sourceTypeName: String, targetTypeName: String) {
        enforceNotNull = true
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = XToValueClassConverter(),
            additionalCode = generateAdditionalCode(),
            additionalConverter = arrayOf(SameTypeConverter(), IntToStringConverter())
        )
    }

    override fun verify(verificationData: VerificationData) {
        val sourceValues = verificationData.sourceVariables.mapIndexed { index, sourceVariable ->
            val sourceTypeName = sourceVariable.second
            val targetTypeName = verificationData.targetVariables[index].second

            when {
                sourceTypeName.endsWith("?") && targetTypeName.endsWith("?") -> null
                sourceTypeName.startsWith("String") -> "123"
                sourceTypeName.startsWith("Int") -> 123
                else -> null
            }
        }
        val sourceInstance = verificationData.sourceKClass.constructors.first().call(*sourceValues.toTypedArray())

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEachIndexed { index, targetVariable ->
            val targetName = targetVariable.first
            val targetTypeName = targetVariable.second
            val targetValue = assertDoesNotThrow {
                verificationData.targetKClass.members.first { it.name == targetName }.call(targetInstance)
            }

            val sourceTypeName = verificationData.sourceVariables[index].second


            val valueClassConstructor = (
                verificationData.targetKClass.members
                    .first { it.name == targetName }
                    .returnType.classifier as KClass<*>
                )
                .primaryConstructor!!


            when {
                sourceTypeName.endsWith("?") && targetTypeName.endsWith("?") -> assertEquals(
                    null,
                    targetValue?.let { it::class.declaredMembers.first { it.name == "value" }.call(targetValue) }
                )

                targetTypeName.startsWith("ValueClassWithAdditionalProperties") -> assertEquals(valueClassConstructor.call("123"), targetValue)
                targetTypeName.startsWith("ValueClassWithNullable") -> assertEquals(valueClassConstructor.call("123"), targetValue)
                targetTypeName.startsWith("SimpleValueClass") -> assertEquals(valueClassConstructor.call("123"), targetValue)
            }
        }
    }

    private fun generateAdditionalCode(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            name = "ValueClasses.kt",
            contents =
                """
@JvmInline
value class SimpleValueClass(val value: String)

@JvmInline
value class ValueClassWithNullable(val value: String?)

@JvmInline
value class ValueClassWithAdditionalProperties(val value: String) {
    val charCount: Int get() = value.length
}
        """.trimIndent()
        )
    )

}
