package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import io.mcarle.konvert.processor.exceptions.NoMatchingTypeConverterException
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertContains
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class XToValueClassConverterITest : ConverterITest() {


    companion object {

        @JvmStatic
        fun sourceAndTargets(): List<Arguments> = listOf(
            //            source,   target,            enforceNotNull
            Arguments.of("String", "SimpleValueClass", false),
            Arguments.of("String", "SimpleValueClass?", false),
            Arguments.of("String?", "SimpleValueClass", true),
            Arguments.of("String?", "SimpleValueClass?", false),
            Arguments.of("Int", "SimpleValueClass", false),
            Arguments.of("Int", "SimpleValueClass?", false),
            Arguments.of("Int?", "SimpleValueClass", true),
            Arguments.of("Int?", "SimpleValueClass?", false),

            Arguments.of("String", "ValueClassWithNullable", false),
            Arguments.of("String", "ValueClassWithNullable?", false),
            Arguments.of("String?", "ValueClassWithNullable", false), // enforceNotNull not needed because value class property is nullable
            Arguments.of("String?", "ValueClassWithNullable?", false),
            Arguments.of("Int", "ValueClassWithNullable", false),
            Arguments.of("Int", "ValueClassWithNullable?", false),
            Arguments.of("Int?", "ValueClassWithNullable", false), // enforceNotNull not needed because value class property is nullable
            Arguments.of("Int?", "ValueClassWithNullable?", false),

            Arguments.of("String", "ValueClassWithAdditionalProperties", false),
            Arguments.of("String", "ValueClassWithAdditionalProperties?", false),
            Arguments.of("String?", "ValueClassWithAdditionalProperties", true),
            Arguments.of("String?", "ValueClassWithAdditionalProperties?", false),
            Arguments.of("Int", "ValueClassWithAdditionalProperties", false),
            Arguments.of("Int", "ValueClassWithAdditionalProperties?", false),
            Arguments.of("Int?", "ValueClassWithAdditionalProperties", true),
            Arguments.of("Int?", "ValueClassWithAdditionalProperties?", false),
        )

    }

    @ParameterizedTest
    @MethodSource("sourceAndTargets")
    fun converterTest(sourceTypeName: String, targetTypeName: String, enforceNotNull: Boolean) {
        this.enforceNotNull = enforceNotNull

        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = XToValueClassConverter(),
            additionalCode = generateAdditionalCode(),
            additionalConverter = arrayOf(SameTypeConverter(), IntToStringConverter())
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["private", "protected", "internal"])
    fun disallowNonPublicConstructorsTest(visibility: String) {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(
                XToValueClassConverter(),
                SameTypeConverter()
            ),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                "Code.kt",
                """
                    import io.mcarle.konvert.api.KonvertTo

                    @JvmInline
                    value class Id $visibility constructor(val value: String)

                    @KonvertTo(Target::class)
                    data class Source(val id: String)
                    data class Target(val id: Id)
                """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            NoMatchingTypeConverterException::class.qualifiedName + ""
        )
    }

    @Test
    fun doNotMatchOnNonValueClasses() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(
                XToValueClassConverter(),
                SameTypeConverter()
            ),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                "Code.kt",
                """
                    import io.mcarle.konvert.api.KonvertTo

                    data class Id(val value: String)

                    @KonvertTo(Target::class)
                    data class Source(val id: String)
                    data class Target(val id: Id)
                """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            NoMatchingTypeConverterException::class.qualifiedName + ""
        )
    }

    @Test
    fun ignoreMultiParamConstructors() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(
                XToValueClassConverter(),
                StringToIntConverter()
            ),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                "Code.kt",
                """
                    import io.mcarle.konvert.api.KonvertTo

                    @JvmInline
                    value class Id private constructor(val value: String) {
                        constructor(year: Int, orderNo: Int? = null): this("${'$'}year-${'$'}orderNo")
                    }

                    @KonvertTo(Target::class)
                    data class Source(val id: String)
                    data class Target(val id: Id)
                """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            NoMatchingTypeConverterException::class.qualifiedName + ""
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun chooseConstructorBasedOnAvailableTypeConverterPriority(availableSameTypeConverter: Boolean) {
        val (compilation) = compileWith(
            enabledConverters = listOfNotNull(
                XToValueClassConverter(),
                if (availableSameTypeConverter) SameTypeConverter() else null,
                IntToStringConverter()
            ),
            code = SourceFile.kotlin(
                "Code.kt",
                """
                    import io.mcarle.konvert.api.KonvertTo

                    @JvmInline
                    value class Id(val value: String) {
                        constructor(value: Int) : this(value.toString())
                    }

                    @KonvertTo(Target::class)
                    data class Source(val id: Int)
                    data class Target(val id: Id)
                """.trimIndent()
            )
        )

        if (availableSameTypeConverter) {
            assertSourceEquals(
                """
                public fun Source.toTarget(): Target = Target(
                  id = Id(id)
                )
                """.trimIndent(), compilation.generatedSourceFor("SourceKonverter.kt")
            )
        } else {
            assertSourceEquals(
                """
                public fun Source.toTarget(): Target = Target(
                  id = Id(id.toString())
                )
                """.trimIndent(), compilation.generatedSourceFor("SourceKonverter.kt")
            )
        }
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

                targetTypeName.startsWith("ValueClassWithAdditionalProperties") -> assertEquals(
                    valueClassConstructor.call("123"),
                    targetValue
                )
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
