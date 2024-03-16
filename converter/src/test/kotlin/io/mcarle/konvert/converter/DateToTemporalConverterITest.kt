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
import java.time.Instant
import java.util.Date

@OptIn(ExperimentalCompilerApi::class)
class DateToTemporalConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            DateToInstantConverter(),
        ).toConverterTestArgumentsWithType {
            "java.util.Date" to it.targetClass.qualifiedName
        }

        private val dateToTemporalConverterClasses: Set<Class<out DateToTemporalConverter>> =
            Reflections(DateToTemporalConverter::class.java)
                .getSubTypesOf(DateToTemporalConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = dateToTemporalConverterClasses.newConverterInstance(simpleConverterName),
        )
    }

    override fun verify(verificationData: VerificationData) {
        val epochMillis = 1674060174913
        val sourceInstance = verificationData.sourceKClass.constructors.first().call(Date(epochMillis))

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            verificationData.targetKClass.members.first { it.name == "test0" }.call(targetInstance)
        }
        when {
            verificationData.targetVariables[0].second.startsWith("java.time.Instant") -> {
                targetValue as Instant
                assertEquals(epochMillis, targetValue.toEpochMilli())
            }
        }
    }

}

