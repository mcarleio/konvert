package io.mcarle.konvert.processor.konvertfrom

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.processor.KonverterITest
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
        private fun allCombinations(): IGenerator<List<String>> = Generator.cartesianProduct(
            // source class
            listOf(
                "public", "private", "internal", "", "java:public", "java:",
            ),
            // target class
            listOf(
                "public", "private", "internal", ""
            ),
            // target class companion
            listOf(
                "public", "private", "internal", "protected", ""
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
        fun validCombinations(): List<Arguments> = allCombinations().filterValid(true).toParameters()

        @JvmStatic
        fun invalidCombinations(): List<Arguments> = allCombinations().filterValid(false).toParameters()
    }

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun checkCorrectVisibilityGenerated(
        sourceClassVisibility: String,
        targetClassVisibility: String,
        targetClassCompanionVisibility: String
    ) {
        val effectiveSourceClassVisibility = sourceClassVisibility.removePrefix("java:")
        val sourceSourceFile = if (sourceClassVisibility.startsWith("java:")) {
            SourceFile.java(
                name = "SourceClass.java",
                contents =
                    """
$effectiveSourceClassVisibility class SourceClass {
    private String property;

    public SourceClass(String property) {
        this.property = property;
    }

    @org.jetbrains.annotations.NotNull
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

        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            code = arrayOf(
                sourceSourceFile,
                SourceFile.kotlin(
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
            )
        )

        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")

        val expectedVisibilityModifier = if ((effectiveSourceClassVisibility != "public" && sourceClassVisibility != "")
            || (targetClassVisibility != "public" && targetClassVisibility != "")
            || (targetClassCompanionVisibility != "public" && targetClassCompanionVisibility != "")
        ) "internal" else "public"

        assertContains(extensionFunctionCode, "$expectedVisibilityModifier fun TargetClass.Companion.fromSourceClass")
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun invalidCombinationsReportedWithUnaccessibleDueToVisibilityClassException(
        sourceClassVisibility: String,
        targetClassVisibility: String,
        targetClassCompanionVisibility: String
    ) {
        val sourceSourceFile = if (sourceClassVisibility.startsWith("java:")) {
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

        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = arrayOf(
                sourceSourceFile,
                SourceFile.kotlin(
                    name = "TargetClass.kt",
                    contents =
                        """
import io.mcarle.konvert.api.KonvertFrom

@KonvertFrom(SourceClass::class)
$targetClassVisibility class TargetClass(val property: String?) {
    $targetClassCompanionVisibility companion object
}
                    """.trimIndent()
                )
            )
        )

        assertContains(compilationResult.messages, "UnaccessibleDueToVisibilityClassException")
    }

}
