package io.mcarle.konvert.processor.konvertfrom

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
class KonvertFromVisibilityITest : KonverterITest() {

    companion object {
        @JvmStatic
        private fun possibleCombinations(): IGenerator<List<String>> = Generator.cartesianProduct(
            // source class (can be java)
            listOf(
                "public", "private", "internal", "java:public", "java:",
            ),
            // target class (cannot be java)
            listOf(
                "public", "private", "internal"
            ),
            // target class companion (cannot be java)
            listOf(
                "public", "private", "internal", "protected"
            )
        )

        private fun Iterable<List<String>>.toParameters(): List<Arguments> = this.map {
            val sourceClassVisibility = it[0]
            val targetClassVisibility = it[1]
            val targetClassCompanionVisibility = it[2]
            Arguments.of(sourceClassVisibility, targetClassVisibility, targetClassCompanionVisibility)
        }

        private fun Iterable<List<String>>.filterValid(valid: Boolean) = this.filter {
            val sourceClassVisibility = it[0]
            val targetClassVisibility = it[1]
            val targetClassCompanionVisibility = it[2]

            if (sourceClassVisibility == "private"
                || targetClassVisibility == "private"
                || targetClassCompanionVisibility == "private"
            ) {
                return@filter !valid
            }

            if (targetClassCompanionVisibility == "protected") {
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
    fun checkCorrectVisibilityGenerated(
        sourceClassVisibility: String,
        targetClassVisibility: String,
        targetClassCompanionVisibility: String
    ) {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            code = arrayOf(
                generateSourceFile(sourceClassVisibility),
                generateTargetFile(targetClassVisibility, targetClassCompanionVisibility)
            )
        )

        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")

        val expectedVisibilityModifier = if (sourceClassVisibility.contains("public")
            && targetClassVisibility == "public"
            && targetClassCompanionVisibility == "public"
        ) "public" else "internal"

        assertContains(extensionFunctionCode, "$expectedVisibilityModifier fun TargetClass.Companion.fromSourceClass")
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun invalidCombinationsReportedWithInaccessibleDueToVisibilityClassException(
        sourceClassVisibility: String,
        targetClassVisibility: String,
        targetClassCompanionVisibility: String
    ) {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = arrayOf(
                generateSourceFile(sourceClassVisibility),
                generateTargetFile(targetClassVisibility, targetClassCompanionVisibility)
            )
        )

        assertContains(compilationResult.messages, "${InaccessibleDueToVisibilityClassException::class.simpleName}")
    }

    private fun generateSourceFile(sourceClassVisibility: String): SourceFile {
        return if (sourceClassVisibility.startsWith("java:")) {
            val effectiveSourceClassVisibility = sourceClassVisibility.removePrefix("java:")
            SourceFile.java(
                name = "SourceClass.java",
                contents =
                    """
$effectiveSourceClassVisibility class SourceClass {
    private String property;

    public SourceClass(String property) {
        this.property = property;
    }

    @ksp.org.jetbrains.annotations.NotNull
    public String getProperty() {
        return property;
    }
}
                """.trimIndent()
            )
        } else {
            SourceFile.kotlin(
                name = "SourceClass.kt",
                contents =
                    """
$sourceClassVisibility class SourceClass(val property: String)
                """.trimIndent()
            )
        }
    }

    private fun generateTargetFile(targetClassVisibility: String, targetClassCompanionVisibility: String): SourceFile {
        return SourceFile.kotlin(
            name = "TargetClass.kt",
            contents =
                """
import io.mcarle.konvert.api.KonvertFrom

@KonvertFrom(SourceClass::class)
$targetClassVisibility class TargetClass(val property: String) {
    $targetClassCompanionVisibility companion object
}
                    """.trimIndent()
        )
    }

}
