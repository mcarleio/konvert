package io.mcarle.konvert.processor.options

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION
import io.mcarle.konvert.converter.api.config.NonConstructorPropertiesMapping
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

    private val entityTarget = SourceFile.kotlin(
        name = "TestCode.kt",
        contents =
            """
import io.mcarle.konvert.api.Konverter

class Source(val id: String, val name: String)
class Entity {
    var id: String = ""
    var name: String = ""
    var note: String? = null
}

@Konverter
interface Mapper {
    fun toEntity(source: Source): Entity
}
            """.trimIndent()
    )

    @Test
    fun `ignore leaves an unmapped property of a created target to its initial value without a word`() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = entityTarget
        )

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toEntity(source: Source): Entity = Entity().also { entity ->
                entity.id = source.id
                entity.name = source.name
              }
            }
            """.trimIndent(),
            compilation.generatedSourceFor("MapperKonverter.kt")
        )
        assertFalse(result.messages.contains("No source for the target"))
    }

    @Test
    fun `warn reports an unmapped property of a created target and keeps its initial value`() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.WARN.name),
            code = entityTarget
        )

        assertContains(result.messages, "No source for the target property 'note' of Entity, its initial value is kept")
        assertFalse(result.messages.contains("target property 'id'"))
        assertFalse(result.messages.contains("target property 'name'"))
        val generated = compilation.generatedSourceFor("MapperKonverter.kt")
        assertContains(generated, "entity.name = source.name")
        assertFalse(generated.contains("entity.note"))
    }

    @Test
    fun `error fails naming every unmapped property of a created target`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.ERROR.name),
            code = entityTarget
        )

        assertContains(result.messages, "No source for the target property(ies) [note] of Entity")
    }

    @Test
    fun `an ignored property, a property with a non-public setter and a val are not reported`() {
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

class Source(val name: String)
open class Entity {
    var id: Long = 0
        protected set
    var version: Int = 0
        private set
    val createdAt: String = "now"
    var name: String = ""
    var note: String? = null
}

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "note", ignore = true)])
    fun toEntity(source: Source): Entity
}
                    """.trimIndent()
            )
        )

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toEntity(source: Source): Entity = Entity().also { entity ->
                entity.name = source.name
              }
            }
            """.trimIndent(),
            compilation.generatedSourceFor("MapperKonverter.kt")
        )
    }

    private val constructorAndProperties = SourceFile.kotlin(
        name = "TestCode.kt",
        contents =
            """
import io.mcarle.konvert.api.Konverter

class Source(val id: String, val name: String)
class Target(val id: String, val note: String? = null) {
    var name: String = ""
    var tag: String = "none"
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
            """.trimIndent()
    )

    @Test
    fun `warn reports unmapped constructor parameters and properties of the same target`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.WARN.name),
            code = constructorAndProperties
        )

        assertContains(result.messages, "No source for the target parameter 'note' of Target, its default value is used")
        assertContains(result.messages, "No source for the target property 'tag' of Target, its initial value is kept")
        assertFalse(result.messages.contains("target property 'name'"))
    }

    @Test
    fun `error fails naming unmapped constructor parameters and properties together`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.ERROR.name),
            code = constructorAndProperties
        )

        assertContains(result.messages, "No source for the target parameter(s) [note] and the target property(ies) [tag] of Target")
    }

    @Test
    fun `a property left unwritten by explicit non-constructor mapping is reported`() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(
                UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.WARN.name,
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to NonConstructorPropertiesMapping.EXPLICIT.name
            ),
            code = entityTarget
        )

        assertContains(result.messages, "No source for the target property 'id' of Entity, its initial value is kept")
        assertContains(result.messages, "No source for the target property 'name' of Entity, its initial value is kept")
        assertContains(result.messages, "No source for the target property 'note' of Entity, its initial value is kept")
        assertContains(compilation.generatedSourceFor("MapperKonverter.kt"), "= Entity()")
    }

    @Test
    fun `a property left unwritten by auto resolving to explicit is reported`() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.WARN.name),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Source(val id: String, val name: String)
class Entity {
    var id: String = ""
    var name: String = ""
}

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(target = "name", source = "id")])
    fun toEntity(source: Source): Entity
}
                    """.trimIndent()
            )
        )

        assertContains(result.messages, "No source for the target property 'id' of Entity, its initial value is kept")
        assertFalse(result.messages.contains("target property 'name'"))
        assertContains(compilation.generatedSourceFor("MapperKonverter.kt"), "entity.name = source.id")
    }

    @Test
    fun `an unmapped java setter of a created target is reported`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            options = mapOf(UNMAPPED_TARGET_POLICY_OPTION.key to UnmappedTargetPolicy.ERROR.name),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "TestCode.kt",
                    contents =
                        """
import io.mcarle.konvert.api.Konverter

class Source(val name: String)

@Konverter
interface Mapper {
    fun toEntity(source: Source): Entity
}
                        """.trimIndent()
                ),
                SourceFile.java(
                    name = "Entity.java",
                    contents =
                        """
public class Entity {
    private String name;
    private String note;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
                        """.trimIndent()
                )
            )
        )

        assertContains(result.messages, "No source for the target property(ies) [note] of Entity")
    }
}
