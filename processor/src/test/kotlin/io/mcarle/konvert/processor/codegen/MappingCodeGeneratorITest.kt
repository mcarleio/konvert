package io.mcarle.konvert.processor.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.ENFORCE_NOT_NULL_OPTION
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.exceptions.IgnoredTargetNotIgnorableException
import io.mcarle.konvert.processor.exceptions.NotNullOperatorNotEnabledException
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains


@Suppress("RedundantVisibilityModifier")
@OptIn(ExperimentalCompilerApi::class)
class MappingCodeGeneratorITest : KonverterITest() {

    @Test
    fun enforceNotNullIfEnabled() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Konfig

@KonvertTo(TargetClass::class, options = [
    Konfig(key = "${ENFORCE_NOT_NULL_OPTION.key}", value = "true")
])
class SourceClass(
    val property: String?
)
class TargetClass(
    var property: String
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property!!
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun throwIfEnforceNotNullNotEnabledButRequired() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(
    val property: String?
)
class TargetClass(
    var property: String,
)
                """.trimIndent()
            )
        )
        assertContains(compilationResult.messages, NotNullOperatorNotEnabledException::class.qualifiedName!!)
    }


    @Test
    fun acceptMissingSourceValuesForConstructorParametersWithDefaultValue() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(
    val property: String,
)
class TargetClass(
    var property: String,
    val otherProperty: String = "Default"
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun acceptMissingSourceValuesForNullableConstructorParameters() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(
    val property: String,
)
class TargetClass(
    var property: String,
    val otherProperty: String?
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property,
              otherProperty = null
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun defaultAndNullPropertiesAreIgnoredWhenNoMappingExists() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class)
class SourceClass
class TargetClass(
    val defaultProperty: String = "Test",
    val nullProperty: String?
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              nullProperty = null
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun throwWhenTargetIgnoredButIsNotIgnorable() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings = [
    Mapping(target = "property", ignore = true)
])
class SourceClass(
    val property: String,
)
class TargetClass(
    var property: String
)
                """.trimIndent()
            )
        )
        assertContains(compilationResult.messages, IgnoredTargetNotIgnorableException::class.qualifiedName!!)
    }

}
