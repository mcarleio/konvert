package io.mcarle.konvert.processor.options

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION
import io.mcarle.konvert.converter.api.config.NonConstructorPropertiesMapping
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.assertDoesNotContain
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertContains
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class NonConstructorPropertiesMappingITest : KonverterITest() {

    @Test
    fun `auto mode is the default mode`() {
        assertEquals(NonConstructorPropertiesMapping.AUTO, NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.defaultValue)
    }

    @Test
    fun `auto (default) mode behaves like implicit when no mappings are declared`() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            verbose = true,
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}

@Konverter
interface MyMapper {
    fun map(source: Source): Target
}
                    """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            "${NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key} resolved to: ${NonConstructorPropertiesMapping.IMPLICIT.name}"
        )
    }

    @Test
    fun `auto (default) mode behaves like implicit when only ignoring mappings are declared`() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            verbose = true,
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(target = "description", ignore = true),
    Mapping(target = "extra", ignore = true),
])
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            "${NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key} resolved to: ${NonConstructorPropertiesMapping.IMPLICIT.name}"
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["constant = \"null\"", "expression = \"null\"", "source = \"description\""])
    fun `auto (default) mode behaves like explicit when at least one non-ignoring mapping is declared`(mappingPart: String) {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            verbose = true,
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(target = "description", ignore = true),
    Mapping(target = "extra", $mappingPart)
])
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            "${NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key} resolved to: ${NonConstructorPropertiesMapping.EXPLICIT.name}"
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["constant = \"null\"", "expression = \"null\"", "source = \"description\""])
    fun `auto (default) mode behaves like explicit when only non-ignoring mappings are declared`(mappingPart: String) {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            verbose = true,
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(target = "extra", $mappingPart)
])
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )

        assertContains(
            compilationResult.messages,
            "${NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key} resolved to: ${NonConstructorPropertiesMapping.EXPLICIT.name}"
        )
    }

    @Test
    fun `implicit mode maps matching source and target properties and ignores additional target properties`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to NonConstructorPropertiesMapping.IMPLICIT.name
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(Target::class)
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )

        val extensionFunctionCode = compilation.generatedSourceFor("SourceKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "target.description = description")
    }

    @Test
    fun `implicit mode maps matching source and target properties and defined mappings`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to NonConstructorPropertiesMapping.IMPLICIT.name
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(target = "extra", source = "description")
])
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )

        val extensionFunctionCode = compilation.generatedSourceFor("SourceKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "target.description = description")
        assertContains(extensionFunctionCode, "target.extra = description")
    }

    @Test
    fun `implicit mode uses explicitly defined mapping over matching source and target properties`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to NonConstructorPropertiesMapping.IMPLICIT.name
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(target = "description", constant = "null")
])
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )

        val extensionFunctionCode = compilation.generatedSourceFor("SourceKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "target.description = null")
    }

    @Test
    fun `explicit mode does not map matching source and target properties when no mappings are defined`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to NonConstructorPropertiesMapping.EXPLICIT.name
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(Target::class)
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )

        val extensionFunctionCode = compilation.generatedSourceFor("SourceKonverter.kt")
        println(extensionFunctionCode)

        assertDoesNotContain(extensionFunctionCode, "description")
    }

    @Test
    fun `explicit mode maps only defined mappings`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to NonConstructorPropertiesMapping.EXPLICIT.name
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(target = "extra", source = "description")
])
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )

        val extensionFunctionCode = compilation.generatedSourceFor("SourceKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "target.extra = description")
        assertDoesNotContain(extensionFunctionCode, "target.description")
    }

    @Test
    fun `all mode fails when a target non-constructor property has no mapping`() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            options = mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to NonConstructorPropertiesMapping.ALL.name
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class)
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )

        assertContains(compilationResult.messages, "Missing mappings for properties: extra.")
    }

    @Test
    fun `all mode considers explicit and implicit mappings`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to NonConstructorPropertiesMapping.ALL.name
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(Target::class, mappings = [
    Mapping(target = "extra", source = "description")
])
class Source(val id: String) {
    var description: String? = null
}

class Target(val id: String) {
    var description: String? = null
    var extra: String? = null
}
                    """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "target.description = description")
        assertContains(extensionFunctionCode, "target.extra = description")
    }

    @ParameterizedTest
    @EnumSource(NonConstructorPropertiesMapping::class)
    fun `every mode works when no non-constructor properties exist`(mode: NonConstructorPropertiesMapping) {
        compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to mode.name
            ),
            code = SourceFile.kotlin(
                name = "test.kt",
                contents =
                    """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(Target::class)
class Source(val id: String)
class Target(val id: String)
                    """.trimIndent()
            )
        )
    }

}
