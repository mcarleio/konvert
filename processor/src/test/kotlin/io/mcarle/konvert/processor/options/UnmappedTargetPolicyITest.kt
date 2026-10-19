package io.mcarle.konvert.processor.options

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.UNMAPPED_TARGET_POLICY_OPTION
import io.mcarle.konvert.converter.api.config.UnmappedTargetPolicy
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCompilerApi::class)
class UnmappedTargetPolicyITest : KonverterITest() {

    private val unmappedDefaults = SourceFile.kotlin(
        name = "TestCode.kt",
        contents =
            """
import io.mcarle.konvert.api.Konverter

class Source(val id: String)
class Target(val id: String, val note: String? = null, val tag: String = "none")

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
            """.trimIndent()
    )

    @Test
    fun `ignore is the default policy`() {
        assertEquals(UnmappedTargetPolicy.IGNORE, UNMAPPED_TARGET_POLICY_OPTION.defaultValue)
    }

    @Test
    fun `ignore leaves an unmapped parameter to its default without a word`() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = unmappedDefaults
        )

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: Source): Target = Target(
                id = source.id
              )
            }
            """.trimIndent(),
            compilation.generatedSourceFor("MapperKonverter.kt")
        )
        assertFalse(result.messages.contains("No source for the target parameter"))
    }

    @Test
    fun `warn reports every unmapped parameter and still uses its default`() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.WARN.name),
            code = unmappedDefaults
        )

        assertContains(result.messages, "No source for the target parameter 'note' of Target, its default value is used")
        assertContains(result.messages, "No source for the target parameter 'tag' of Target, its default value is used")
        assertContains(compilation.generatedSourceFor("MapperKonverter.kt"), "Target(\n    id = source.id\n  )")
    }

    @Test
    fun `error fails naming every unmapped parameter`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.ERROR.name),
            code = unmappedDefaults
        )

        assertContains(result.messages, "No source for the target parameter(s) [note, tag] of Target")
    }

    @Test
    fun `error fails for a nullable parameter without a default too`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.ERROR.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class Source(val id: String)
class Target(val id: String, val note: String?)

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
                    """.trimIndent()
            )
        )

        assertContains(result.messages, "No source for the target parameter(s) [note] of Target")
    }

    @Test
    fun `an ignored parameter is not reported and keeps its default`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.ERROR.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Source(val id: String)
class Target(val id: String, val note: String? = null, val tag: String = "none")

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "note", ignore = true), Mapping(target = "tag", ignore = true)])
    fun toTarget(source: Source): Target
}
                    """.trimIndent()
            )
        )

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: Source): Target = Target(
                id = source.id
              )
            }
            """.trimIndent(),
            compilation.generatedSourceFor("MapperKonverter.kt")
        )
    }
}
