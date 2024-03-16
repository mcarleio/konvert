package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class XToDateConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            StringToDateConverter(),
            LongEpochMillisToDateConverter(),
            LongEpochSecondsToDateConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to "java.util.Date"
        }

        private val xToDateConverterClasses: Set<Class<out XToDateConverter>> = Reflections(XToDateConverter::class.java)
            .getSubTypesOf(XToDateConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = xToDateConverterClasses.newConverterInstance(simpleConverterName)
        )
    }

    override fun verify(verificationData: VerificationData) {
        val instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceTypeName = sourceVariable.second
            when {
                sourceTypeName.startsWith("kotlin.String") -> instant.toString()
                sourceTypeName.startsWith("kotlin.Long") -> when (verificationData.converter) {
                    is LongEpochMillisToDateConverter -> instant.toEpochMilli()
                    is LongEpochSecondsToDateConverter -> instant.toEpochMilli() / 1000
                    else -> null
                }

                else -> null
            }
        }
        val sourceInstance = verificationData.sourceKClass.constructors.first().call(*sourceValues.toTypedArray())

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEach { targetVariable ->
            val targetName = targetVariable.first
            val targetValue = assertDoesNotThrow {
                verificationData.targetKClass.members.first { it.name == targetName }.call(targetInstance)
            }
            assertEquals(Date(instant.toEpochMilli()), targetValue)
        }
    }

}

