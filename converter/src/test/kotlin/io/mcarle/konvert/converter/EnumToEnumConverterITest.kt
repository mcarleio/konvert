package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass

@OptIn(ExperimentalCompilerApi::class)
class EnumToEnumConverterITest : ConverterITest() {

    companion object {

        @JvmStatic
        fun types(): List<Arguments> = listOf("MyFirstEnum").toConverterTestArguments {
            it to "MySecondEnum"
        }
    }

    @ParameterizedTest
    @MethodSource("types")
    fun converterTest(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = EnumToEnumConverter(),
            additionalCode = this.generateAdditionalCode()
        )
    }

    @ParameterizedTest
    @MethodSource("types")
    fun missingEnumValues(sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = targetTypeName,
            targetTypeName = sourceTypeName,
            converter = EnumToEnumConverter(),
            expectedResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            additionalCode = this.generateAdditionalCode()
        )
    }

    @Test
    fun enumsInDifferentPackages() {
        executeTest(
            sourceTypeName = "a.OtherEnum",
            targetTypeName = "b.OtherEnum",
            converter = EnumToEnumConverter(),
            additionalCode = this.generateAdditionalCode()
        )
    }

    private fun generateAdditionalCode(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            name = "MyEnums.kt",
            contents =
            """
enum class FirstEnum {
    XXX,
    YYY,
    ZZZ
}
enum class SecondEnum {
    AAA,
    ZZZ,
    XXX,
    YYY,
}
typealias MyFirstEnum = FirstEnum
typealias MySecondEnum = SecondEnum
        """.trimIndent()
        ), SourceFile.kotlin(
            name = "a/OtherEnum.kt",
            contents = """
package a

enum class OtherEnum {
    AAA,
    BBB,
    CCC
}
        """.trimIndent()
        ), SourceFile.kotlin(
            name = "b/OtherEnum.kt",
            contents = """
package b

enum class OtherEnum {
    AAA,
    BBB,
    CCC,
    DDD
}
        """.trimIndent()
        )
    )

    override fun verify(verificationData: VerificationData) {
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceVariableName = sourceVariable.first
            (verificationData.sourceKClass.members.first { it.name == sourceVariableName }.returnType.classifier as KClass<*>).java
                .enumConstants.random() as Enum<*>
        }
        val sourceInstance = verificationData.sourceKClass.constructors.first().call(*sourceValues.toTypedArray())

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEachIndexed { index, targetVariable ->
            val targetVariableName = targetVariable.first
            val targetValue = assertDoesNotThrow {
                verificationData.targetKClass.members.first { it.name == targetVariableName }.call(targetInstance)
            }
            assertEquals(sourceValues[index].toString(), targetValue.toString())
        }
    }

}

