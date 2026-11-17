package io.mcarle.konvert.processor.options

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.IGNORED_TARGET_POLICY_OPTION
import io.mcarle.konvert.converter.api.config.IgnoredTargetPolicy
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCompilerApi::class)
class IgnoredTargetPolicyITest : KonverterITest() {

    private val staleIgnore = SourceFile.kotlin(
        name = "TestCode.kt",
        contents =
            """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Source(val id: String, val note: String?, val tag: String)
class Target(val id: String, val note: String? = null, val tag: String = "none")

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "note", ignore = true), Mapping(target = "tag", ignore = true)])
    fun toTarget(source: Source): Target
}
            """.trimIndent()
    )

    @Test
    fun `ignore is the default policy`() {
        assertEquals(IgnoredTargetPolicy.IGNORE, IGNORED_TARGET_POLICY_OPTION.defaultValue)
    }

    @Test
    fun `ignore drops an ignored target the source provides without a word`() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = staleIgnore
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
        assertFalse(result.messages.contains("would map to"))
    }

    @Test
    fun `warn reports every ignored target the source provides and still ignores it`() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(IGNORED_TARGET_POLICY_OPTION.key to IgnoredTargetPolicy.WARN.name),
            code = staleIgnore
        )

        assertContains(
            result.messages,
            "Mapper.toTarget ignores the target 'note', but the source property 'note' would map to it: " +
                "remove @Mapping(target = \"note\", ignore = true), or map it"
        )
        assertContains(
            result.messages,
            "Mapper.toTarget ignores the target 'tag', but the source property 'tag' would map to it"
        )
        assertContains(compilation.generatedSourceFor("MapperKonverter.kt"), "Target(\n    id = source.id\n  )")
    }

    @Test
    fun `error fails naming the function and every ignored target the source provides`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(IGNORED_TARGET_POLICY_OPTION.key to IgnoredTargetPolicy.ERROR.name),
            code = staleIgnore
        )

        assertContains(
            result.messages,
            "Mapper.toTarget ignores target(s) that a source would map to: " +
                "'note' from the source property 'note', 'tag' from the source property 'tag'. " +
                "Remove the @Mapping(target = \"...\", ignore = true), or map them"
        )
    }

    @Test
    fun `an ignored target without a source of the same name is never reported`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(IGNORED_TARGET_POLICY_OPTION.key to IgnoredTargetPolicy.ERROR.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Source(val id: String)
class Target(val id: String, val note: String? = null)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "note", ignore = true)])
    fun toTarget(source: Source): Target
}
                    """.trimIndent()
            )
        )

        assertContains(compilation.generatedSourceFor("MapperKonverter.kt"), "Target(\n    id = source.id\n  )")
    }

    @Test
    fun `an ignored target whose same-named source cannot be converted is not reported`() {
        compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(IGNORED_TARGET_POLICY_OPTION.key to IgnoredTargetPolicy.ERROR.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Source(val id: String, val note: Int)
class Target(val id: String, val note: String? = null)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "note", ignore = true)])
    fun toTarget(source: Source): Target
}
                    """.trimIndent()
            )
        )
    }

    @Test
    fun `a source property another mapping renames is not reported`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(IGNORED_TARGET_POLICY_OPTION.key to IgnoredTargetPolicy.ERROR.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Source(val id: String, val note: String)
class Target(val id: String, val comment: String, val note: String? = null)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "comment", source = "note"), Mapping(target = "note", ignore = true)])
    fun toTarget(source: Source): Target
}
                    """.trimIndent()
            )
        )

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: Source): Target = Target(
                id = source.id,
                comment = source.note
              )
            }
            """.trimIndent(),
            compilation.generatedSourceFor("MapperKonverter.kt")
        )
    }

    @Test
    fun `an additional parameter of the same name counts as a source`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(IGNORED_TARGET_POLICY_OPTION.key to IgnoredTargetPolicy.ERROR.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Source(val id: String)
class Target(val id: String, val note: String? = null)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "note", ignore = true)])
    fun toTarget(@Konverter.Source source: Source, note: String): Target
}
                    """.trimIndent()
            )
        )

        assertContains(result.messages, "Mapper.toTarget ignores target(s) that a source would map to: 'note' from the parameter 'note'")
    }

    @Test
    fun `KonvertTo and KonvertFrom report the generated function`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(IGNORED_TARGET_POLICY_OPTION.key to IgnoredTargetPolicy.WARN.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [Mapping(target = "note", ignore = true)])
class Source(val id: String, val note: String)

class Target(val id: String, val note: String? = null) {
    @KonvertFrom(Source::class, mappings = [Mapping(target = "note", ignore = true)])
    companion object
}
                    """.trimIndent()
            )
        )

        assertContains(result.messages, "Source.toTarget ignores the target 'note', but the source property 'note' would map to it")
        assertContains(result.messages, "Target.Companion.fromSource ignores the target 'note', but the source property 'note' would map to it")
    }

    @Test
    fun `a Konfig on the annotation overrides the global policy`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(IGNORED_TARGET_POLICY_OPTION.key to IgnoredTargetPolicy.WARN.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Source(val id: String, val note: String)
class Target(val id: String, val note: String? = null)

@Konverter(options = [Konfig(key = "konvert.ignored-target-policy", value = "error")])
interface Mapper {
    @Konvert(
        mappings = [Mapping(target = "note", ignore = true)],
        options = [Konfig(key = "konvert.ignored-target-policy", value = "ignore")]
    )
    fun toTargetKnowingly(source: Source): Target

    @Konvert(mappings = [Mapping(target = "note", ignore = true)])
    fun toTarget(source: Source): Target
}
                    """.trimIndent()
            )
        )

        assertFalse(result.messages.contains("Mapper.toTargetKnowingly ignores"))
        assertContains(result.messages, "Mapper.toTarget ignores target(s) that a source would map to: 'note' from the source property 'note'")
    }
}
