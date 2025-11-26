package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.api.config.ENFORCE_NOT_NULL_STRATEGY_OPTION
import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.Date
import kotlin.test.Test

@OptIn(ExperimentalCompilerApi::class)
class SameTypeConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun sourceAndTargets(): List<Arguments> = listOf(
            "String",
            "Int",
            "UInt",
            "Long",
            "ULong",
            "Short",
            "UShort",
            "Float",
            "Double",
            "Byte",
            "UByte",
            "Char",
            "Boolean",
            "java.util.Date",
            "Any",
            "List<String>",
            "List<String?>",
        ).toConverterTestArguments {
            it to it
        }
    }


    @ParameterizedTest
    @MethodSource("sourceAndTargets")
    fun converterTest(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = SameTypeConverter()
        )
    }

    private fun validateGeneratedSourceCode(
        verificationData: VerificationData
    ) {
        val code = verificationData.generatedCode
        val sourceVariables = verificationData.sourceVariables
        val targetVariables = verificationData.targetVariables
        val variables = sourceVariables.mapIndexed { index, source ->
            source.first to if (source.second.endsWith("?") && !targetVariables[index].second.endsWith("?")) "!!" else ""
        }
        assertSourceEquals(
            expected = """
                public object FooMapperImpl : FooMapper {
                  override fun toYyy(it: Xxx): Yyy = Yyy(
                    ${variables.joinToString(",\n    ") { "${it.first} = it.${it.first}${it.second}" }}
                  )
                }
            """.trimIndent(),
            generatedCode = code
        )
    }

    override fun verify(verificationData: VerificationData) {
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceTypeName = sourceVariable.second
            when {
                sourceTypeName.startsWith("String") -> "Test"
                sourceTypeName.startsWith("Int") -> -123
                sourceTypeName.startsWith("UInt") -> 123.toUInt()
                sourceTypeName.startsWith("Long") -> -333L
                sourceTypeName.startsWith("ULong") -> 555.toULong()
                sourceTypeName.startsWith("Short") -> 123.toShort()
                sourceTypeName.startsWith("UShort") -> (-123).toUShort()
                sourceTypeName.startsWith("Float") -> 12.34f
                sourceTypeName.startsWith("Double") -> 3.141
                sourceTypeName.startsWith("Byte") -> (-123).toByte()
                sourceTypeName.startsWith("UByte") -> 123.toUByte()
                sourceTypeName.startsWith("Char") -> 'M'
                sourceTypeName.startsWith("Boolean") -> true
                sourceTypeName.startsWith("java.util.Date") -> Date()
                sourceTypeName.startsWith("Any") -> mapOf("Hallo" to "Welt")
                sourceTypeName.startsWith("List<String>") -> listOf("YOOOO")
                sourceTypeName.startsWith("List<String?>") -> listOf(null, "pointer")
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
            val sourceValue = sourceValues[index]
            when {
                (sourceTypeName.endsWith("?") xor targetTypeName.endsWith("?"))
                    || listOf("String", "List", "java", "Any").none { sourceTypeName.startsWith(it) } ->
                    assertEquals(sourceValue, targetValue)
                else -> assertSame(sourceValue, targetValue)
            }
        }

        this.validateGeneratedSourceCode(verificationData)
    }

    @Test
    fun converterUsesRequireNotNullForNullableToNonNull() {
        enforceNotNull = true
        enforceNotNullStrategy = "REQUIRE_NOT_NULL"

        executeTest(
            typeNamePairs = listOf("String?" to "String"),
            converter = SameTypeConverter(),
            verification = { verificationData ->
                val code = verificationData.generatedCode
                assertSourceEquals(
                    """
                    public object FooMapperImpl : FooMapper {
                      override fun toYyy(it: Xxx): Yyy = Yyy(
                        test0 = requireNotNull(it.test0) { "Value for 'it.test0' must not be null" }
                      )
                    }
                    """.trimIndent(),
                    code
                )
            }
        )
    }


}

