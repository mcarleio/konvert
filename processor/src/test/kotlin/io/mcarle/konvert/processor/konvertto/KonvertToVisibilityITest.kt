package io.mcarle.konvert.processor.konvertto

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.exceptions.InaccessibleDueToVisibilityClassException
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.paukov.combinatorics3.Generator
import org.paukov.combinatorics3.IGenerator
import kotlin.test.assertContains

@OptIn(ExperimentalCompilerApi::class)
class KonvertToVisibilityITest : KonverterITest() {


    companion object {
        @JvmStatic
        private fun possibleCombinations(): IGenerator<List<String>> = Generator.cartesianProduct(
            // source class (cannot be java)
            listOf(
                "public", "private", "internal"
            ),
            // target class (can be java)
            listOf(
                "public", "private", "internal", "java:public", "java:"
            )
        )

        private fun Iterable<List<String>>.toParameters(): List<Arguments> = this.map {
            val sourceClassVisibility = it[0]
            val targetClassVisibility = it[1]
            Arguments.of(sourceClassVisibility, targetClassVisibility)
        }

        private fun Iterable<List<String>>.filterValid(valid: Boolean) = this.filter {
            val sourceClassVisibility = it[0]
            val targetClassVisibility = it[1]

            if (sourceClassVisibility == "private"
                || targetClassVisibility == "private"
            ) {
                return@filter !valid
            }

            valid
        }

        @JvmStatic
        fun validCombinations(): List<Arguments> = possibleCombinations().filterValid(true).toParameters()

        @JvmStatic
        fun invalidCombinations(): List<Arguments> = possibleCombinations().filterValid(false).toParameters()
    }


    @ParameterizedTest
    @MethodSource("validCombinations")
    fun checkCorrectVisibilityGenerated(sourceClassVisibility: String, targetClassVisibility: String) {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            code = arrayOf(
                generateSourceFile(sourceClassVisibility),
                generateTargetFile(targetClassVisibility)
            )
        )

        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")

        val expectedVisibilityModifier = if (sourceClassVisibility == "public" && targetClassVisibility.contains("public")) {
            "public"
        } else {
            "internal"
        }

        assertContains(extensionFunctionCode, "$expectedVisibilityModifier fun SourceClass.toTargetClass")
    }


    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun invalidCombinationsReportedWithInaccessibleDueToVisibilityClassException(
        sourceClassVisibility: String,
        targetClassVisibility: String,
    ) {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = arrayOf(
                generateSourceFile(sourceClassVisibility),
                generateTargetFile(targetClassVisibility)
            )
        )

        assertContains(compilationResult.messages, "${InaccessibleDueToVisibilityClassException::class.simpleName}")
    }

    private fun generateSourceFile(sourceClassVisibility: String): SourceFile {
        return SourceFile.kotlin(
            name = "SourceClass.kt",
            contents = """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
$sourceClassVisibility class SourceClass(val property: String)
            """.trimIndent()
        )
    }

    private fun generateTargetFile(targetClassVisibility: String): SourceFile {
        return if (targetClassVisibility.startsWith("java:")) {
            val effectiveTargetClassVisibility = targetClassVisibility.removePrefix("java:")
            SourceFile.java(
                name = "TargetClass.java",
                contents = """
$effectiveTargetClassVisibility class TargetClass {
    private String property;

    public TargetClass(String property) {
        this.property = property;
    }
}
                """.trimIndent()
            )
        } else {
            SourceFile.kotlin(
                name = "TargetClass.kt",
                contents =
                    """
$targetClassVisibility class TargetClass(val property: String)
                """.trimIndent()
            )
        }
    }

}
