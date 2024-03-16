package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import kotlin.test.assertIs

@OptIn(ExperimentalCompilerApi::class)
class TemporalToDateConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            InstantToDateConverter(),
            ZonedDateTimeToDateConverter(),
            OffsetDateTimeToDateConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to "java.util.Date"
        }

        private val temporalToDateConverterClasses: Set<Class<out TemporalToDateConverter>> =
            Reflections(TemporalToDateConverter::class.java)
                .getSubTypesOf(TemporalToDateConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = temporalToDateConverterClasses.newConverterInstance(simpleConverterName)
        )
    }

    override fun verify(verificationData: VerificationData) {
        val instant = Instant.now()
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceTypeName = sourceVariable.second
            when {
                sourceTypeName.startsWith("java.time.Instant") -> instant
                sourceTypeName.startsWith("java.time.ZonedDateTime") -> instant.atOffset(ZoneOffset.UTC).toZonedDateTime()
                sourceTypeName.startsWith("java.time.OffsetDateTime") -> instant.atOffset(ZoneOffset.UTC)
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
            assertIs<Date>(targetValue)
            Assertions.assertEquals(Date.from(instant), targetValue)
        }
    }

}

