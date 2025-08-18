package io.mcarle.konvert.converter.api.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class ConfigurationExtensionsTest {

    companion object {

        @JvmStatic
        fun validValuesForNonConstructorPropertiesMapping(): List<Arguments> {
            return listOf(
                Arguments.of("auto", NonConstructorPropertiesMapping.AUTO),
                Arguments.of("all", NonConstructorPropertiesMapping.ALL),
                Arguments.of("explicit", NonConstructorPropertiesMapping.EXPLICIT),
                Arguments.of("implicit", NonConstructorPropertiesMapping.IMPLICIT),
                Arguments.of("AUTO", NonConstructorPropertiesMapping.AUTO),
                Arguments.of("ALL", NonConstructorPropertiesMapping.ALL),
                Arguments.of("EXPLICIT", NonConstructorPropertiesMapping.EXPLICIT),
                Arguments.of("IMPLICIT", NonConstructorPropertiesMapping.IMPLICIT),
            )
        }

        @JvmStatic
        fun validValuesForInvalidMappingStrategy(): List<Arguments> {
            return listOf(
                Arguments.of("warn", InvalidMappingStrategy.WARN),
                Arguments.of("fail", InvalidMappingStrategy.FAIL),
                Arguments.of("WARN", InvalidMappingStrategy.WARN),
                Arguments.of("FAIL", InvalidMappingStrategy.FAIL)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("validValuesForNonConstructorPropertiesMapping")
    fun `valid values for nonConstructorPropertiesMapping are correctly mapped to enum`(
        configValue: String,
        expectedResult: NonConstructorPropertiesMapping
    ) {
        withIsolatedConfiguration(
            mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to configValue
            )
        ) {
            val parsedValue = Configuration.nonConstructorPropertiesMapping

            assertEquals(expectedResult, parsedValue)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid", "_", ""])
    fun `invalid values for nonConstructorPropertiesMapping are mapped to default`(configValue: String) {
        withIsolatedConfiguration(
            mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to configValue
            )
        ) {
            val parsedValue = Configuration.nonConstructorPropertiesMapping

            assertEquals(NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.defaultValue, parsedValue)
        }
    }

    @Test
    fun `default value for nonConstructorPropertiesMapping is AUTO`() {
        assertEquals(NonConstructorPropertiesMapping.AUTO, NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.defaultValue)
    }

    @ParameterizedTest
    @MethodSource("validValuesForInvalidMappingStrategy")
    fun `valid values for invalidMappingStrategy are correctly mapped to enum`(
        configValue: String,
        expectedResult: InvalidMappingStrategy
    ) {
        withIsolatedConfiguration(
            mapOf(
                INVALID_MAPPING_STRATEGY_OPTION.key to configValue
            )
        ) {
            val parsedValue = Configuration.invalidMappingStrategy

            assertEquals(expectedResult, parsedValue)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid", "_", ""])
    fun `invalid values for invalidMappingStrategy are mapped to default`(configValue: String) {
        withIsolatedConfiguration(
            mapOf(
                INVALID_MAPPING_STRATEGY_OPTION.key to configValue
            )
        ) {
            val parsedValue = Configuration.invalidMappingStrategy

            assertEquals(INVALID_MAPPING_STRATEGY_OPTION.defaultValue, parsedValue)
        }
    }

    @Test
    fun `default value for invalidMappingStrategy is WARN`() {
        assertEquals(InvalidMappingStrategy.WARN, INVALID_MAPPING_STRATEGY_OPTION.defaultValue)
    }
}
