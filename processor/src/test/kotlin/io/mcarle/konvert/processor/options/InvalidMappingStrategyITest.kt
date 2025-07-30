package io.mcarle.konvert.processor.options

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.INVALID_MAPPING_STRATEGY_OPTION
import io.mcarle.konvert.converter.api.config.InvalidMappingStrategy
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.exceptions.InvalidMappingException
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class InvalidMappingStrategyITest : KonverterITest() {


    @Test
    fun `warn mode is the default mode`() {
        assertEquals(InvalidMappingStrategy.WARN, INVALID_MAPPING_STRATEGY_OPTION.defaultValue)
    }

    @Test
    fun `warn mode logs a warning message when mapping defines unknown source`() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                INVALID_MAPPING_STRATEGY_OPTION.key to InvalidMappingStrategy.WARN.name,
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(source = "does_not_exist", target = "id")
])
class Source(val id: String)
class Target(val id: String)
                """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            "Ignoring the mapping @io.mcarle.konvert.api.Mapping(target=id, source=does_not_exist, constant=, expression=, ignore=false, enable=[]) as the source field 'does_not_exist' does not exist."
        )
    }

    @Test
    fun `warn mode logs a warning message when mapping defines unknown target`() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                INVALID_MAPPING_STRATEGY_OPTION.key to InvalidMappingStrategy.WARN.name,
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(target = "does_not_exist", constant = "null")
])
class Source(val id: String)
class Target(val id: String)
                """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            "Ignoring the mapping @io.mcarle.konvert.api.Mapping(target=does_not_exist, source=, constant=null, expression=, ignore=false, enable=[]) as the target field 'does_not_exist' does not exist."
        )
    }

    @Test
    fun `fail mode fails compilation when mapping defines unknown source`() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            options = mapOf(
                INVALID_MAPPING_STRATEGY_OPTION.key to InvalidMappingStrategy.FAIL.name,
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(source = "does_not_exist", target = "id")
])
class Source(val id: String)
class Target(val id: String)
                """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            InvalidMappingException::class.qualifiedName.toString()
        )
        assertContains(
            compilationResult.messages,
            "The referenced source field(s) [does_not_exist] do not exist"
        )
    }

    @Test
    fun `fail mode fails compilation when mapping defines unknown target`() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            options = mapOf(
                INVALID_MAPPING_STRATEGY_OPTION.key to InvalidMappingStrategy.FAIL.name,
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(target = "does_not_exist", constant = "null")
])
class Source(val id: String)
class Target(val id: String)
                """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            InvalidMappingException::class.qualifiedName.toString()
        )
        assertContains(
            compilationResult.messages,
            "The referenced target field(s) [does_not_exist] do not exist"
        )
    }

}
