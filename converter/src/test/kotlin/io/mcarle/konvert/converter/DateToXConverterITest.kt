package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import java.util.Date

@OptIn(ExperimentalCompilerApi::class)
class DateToXConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            DateToStringConverter(),
            DateToLongEpochMillisConverter(),
            DateToLongEpochSecondsConverter(),
        ).toConverterTestArgumentsWithType {
            "java.util.Date" to it.targetClass.qualifiedName
        }

        private val dateToXConverterClasses: Set<Class<out DateToXConverter>> =
            Reflections(DateToXConverter::class.java)
                .getSubTypesOf(DateToXConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            dateToXConverterClasses.newConverterInstance(simpleConverterName),
        )
    }

    override fun verify(verificationData: VerificationData) {
        val epochMillis = 1674060174913L
        val sourceValues = verificationData.sourceVariables.map { Date(epochMillis) }

        val sourceInstance = verificationData.sourceKClass.constructors.first().call(*sourceValues.toTypedArray())

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEach { targetVariable ->
            val targetVariableName = targetVariable.first
            val targetVariableType = targetVariable.second
            val targetValue = assertDoesNotThrow {
                verificationData.targetKClass.members.first { it.name == targetVariableName }.call(targetInstance)
            }
            when {
                targetVariableType.startsWith("kotlin.String") -> {
                    targetValue as String
                    assertEquals("2023-01-18T16:42:54.913Z", targetValue)
                }
                targetVariableType.startsWith("kotlin.Long") -> {
                    targetValue as Long
                    when (verificationData.converter) {
                        is DateToLongEpochMillisConverter -> assertEquals(epochMillis, targetValue)
                        is DateToLongEpochSecondsConverter -> assertEquals(epochMillis / 1000, targetValue)
                    }
                }
            }
        }
    }

}

