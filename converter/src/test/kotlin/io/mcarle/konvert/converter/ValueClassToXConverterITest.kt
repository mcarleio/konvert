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
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class ValueClassToXConverterITest : ConverterITest() {


    companion object {

        @JvmStatic
        fun sourceAndTargets(): List<Arguments> = listOf(
            "SimpleValueClass",
            "ValueClassWithNullable",
            "ValueClassWithAdditionalProperties"
        ).joinConverterTestArguments(
            "String",
            "Int",
        )

    }

    @ParameterizedTest
    @MethodSource("sourceAndTargets")
    fun converterTest(sourceTypeName: String, targetTypeName: String) {
        enforceNotNull = true
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = ValueClassToXConverter(),
            additionalCode = generateAdditionalCode(),
            additionalConverter = arrayOf(SameTypeConverter(), StringToIntConverter())
        )
    }

    override fun verify(verificationData: VerificationData) {
        val sourceValues = verificationData.sourceVariables.mapIndexed { index, sourceVariable ->
            val sourceTypeName = sourceVariable.second

            val valueClassConstructor = (
                verificationData.sourceKClass.members
                    .first { it.name == sourceVariable.first }
                    .returnType.classifier as KClass<*>
                )
                .primaryConstructor!!

            when {
                sourceTypeName.startsWith("ValueClassWithAdditionalProperties") ->
                    if (sourceTypeName.endsWith("?") && verificationData.targetVariables[index].second.endsWith("?")) {
                        null
                    } else {
                        valueClassConstructor.call("123")
                    }
                sourceTypeName.startsWith("ValueClassWithNullable") ->
                    if (sourceTypeName.endsWith("?") && verificationData.targetVariables[index].second.endsWith("?")) {
                        valueClassConstructor.call(null)
                    } else {
                        valueClassConstructor.call("123")
                    }
                sourceTypeName.startsWith("SimpleValueClass") ->
                    if (sourceTypeName.endsWith("?") && verificationData.targetVariables[index].second.endsWith("?")) {
                        null
                    } else {
                        valueClassConstructor.call("123")
                    }
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

            when {
                sourceTypeName.endsWith("?") && targetTypeName.endsWith("?") -> assertEquals(null, targetValue)
                targetTypeName.startsWith("String") -> assertEquals("123", targetValue)
                targetTypeName.startsWith("Int") -> assertEquals(123, targetValue)
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
