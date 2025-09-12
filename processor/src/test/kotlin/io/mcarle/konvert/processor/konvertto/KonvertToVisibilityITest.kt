package io.mcarle.konvert.processor.konvertto

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.exceptions.UnaccessibleDueToVisibilityClassException
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertContains

@OptIn(ExperimentalCompilerApi::class)
class KonvertToVisibilityITest : KonverterITest() {

    @ParameterizedTest
    @ValueSource(strings = ["internal", "public", ""])
    fun alignVisibilityAccordingToSourceClass(sourceVisibilityModifier: String) {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(DefaultTargetClass::class)
@KonvertTo(PublicTargetClass::class)
$sourceVisibilityModifier class SourceClass(val property: String)

class DefaultTargetClass(val property: String)
public class PublicTargetClass(val property: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        val expectedVisibilityModifier = if (sourceVisibilityModifier == "") "public" else sourceVisibilityModifier

        assertSourceEquals(
            """
            $expectedVisibilityModifier fun SourceClass.toDefaultTargetClass(): DefaultTargetClass = DefaultTargetClass(
              property = property
            )

            $expectedVisibilityModifier fun SourceClass.toPublicTargetClass(): PublicTargetClass = PublicTargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["internal", "public", ""])
    fun alignVisibilityAccordingToTargetClass(targetVisibilityModifier: String) {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class DefaultSourceClass(val property: String)

@KonvertTo(TargetClass::class)
public class PublicSourceClass(val property: String)

$targetVisibilityModifier class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val expectedVisibilityModifier = if (targetVisibilityModifier == "") "public" else targetVisibilityModifier

        val extensionFunctionCodeForDefault = compilation.generatedSourceFor("DefaultSourceClassKonverter.kt")
        println(extensionFunctionCodeForDefault)

        assertSourceEquals(
            """
            $expectedVisibilityModifier fun DefaultSourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCodeForDefault
        )

        val extensionFunctionCodeForPublic = compilation.generatedSourceFor("PublicSourceClassKonverter.kt")
        println(extensionFunctionCodeForPublic)

        assertSourceEquals(
            """
            $expectedVisibilityModifier fun PublicSourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCodeForPublic
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["internal", "public", ""])
    fun alignVisibilityAccordingToSourceAndTargetClass(visibilityModifier: String) {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
$visibilityModifier class SourceClass(val property: String)
$visibilityModifier class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val expectedVisibilityModifier = if (visibilityModifier == "") "public" else visibilityModifier

        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            $expectedVisibilityModifier fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["source", "target", "both"])
    fun throwUnaccessibleDueToVisibilityClassExceptionWhenPrivateVisibility(privateSelector: String) {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
${if (privateSelector != "target") "private" else ""} class SourceClass(val property: String)
${if (privateSelector != "source") "private" else ""} class TargetClass(val property: String)
                """.trimIndent()
            )
        )

        if (privateSelector == "target") {
            assertContains(
                compilationResult.messages,
                "${UnaccessibleDueToVisibilityClassException::class.simpleName}: The class TargetClass is not accessible due to its PRIVATE visibility"
            )
        } else {
            assertContains(
                compilationResult.messages,
                "${UnaccessibleDueToVisibilityClassException::class.simpleName}: The class SourceClass is not accessible due to its PRIVATE visibility"
            )
        }
    }

}
