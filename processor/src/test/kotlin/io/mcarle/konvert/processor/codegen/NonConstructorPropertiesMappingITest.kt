package io.mcarle.konvert.processor.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("RedundantVisibilityModifier")
@OptIn(ExperimentalCompilerApi::class)
class NonConstructorPropertiesMappingITest : KonverterITest() {

    @Test
    fun `auto mode generates also block with matched non-constructor property`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "auto",
            ),
            code = SourceFile.kotlin(
                name = "TestFooBarMapperKonverter.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class Foo(val id: String) {
    var description: String? = null
}

class Bar(val id: String) {
    var description: String? = null
    var extra: String? = null
}

@Konverter
interface FooBarMapper {
    fun map(source: Foo): Bar
}
                    """.trimIndent()
            )
        )

        val generated = compilation.generatedSourceFor("FooBarMapperKonverter.kt")

        assertSourceEquals(
            """
            public object FooBarMapperImpl : FooBarMapper {
              override fun map(source: Foo): Bar = Bar(
                id = source.id
              ).also { bar ->
                bar.description = source.description
              }
            }
            """.trimIndent(),
            generated
        )
    }

    @Test
    fun `strict mode passes if only constructor parameters are present and matched`() {
        val (compilation, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "explicit",
            ),
            code = SourceFile.kotlin(
                name = "StrictConstructorOnly.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class Foo(val id: String)

class Bar(val id: String)

@Konverter
interface ConstructorOnlyMapper {
    fun map(source: Foo): Bar
}
                """.trimIndent()
            )
        )

        val generated = compilation.generatedSourceFor("ConstructorOnlyMapperKonverter.kt")

        assertTrue("id = source.id" in generated)
        assertFalse("also" in generated)
    }

    @Test
    fun `strict mode maps only explicitly listed non-constructor property`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "explicit",
            ),
            code = SourceFile.kotlin(
                name = "StrictExplicit.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konfig
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Foo(val id: String) {
    var description: String? = null
}

class Bar(val id: String) {
    var description: String? = null
    var extra: String? = null
}

@Konverter
interface StrictExplicitMapper {
    @Konvert(
        mappings = [Mapping(target = "description", source = "description")]
    )
    fun map(source: Foo): Bar
}
                """.trimIndent()
            )
        )

        val generated = compilation.generatedSourceFor("StrictExplicitMapperKonverter.kt")

        assertTrue("description = source.description" in generated)
        assertFalse("extra =" in generated) // unmapped, but ignored
    }

    @Test
    fun `strict mode with no constructor and no mappings uses fallback`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "auto",
            ),
            code = SourceFile.kotlin(
                name = "StrictFallback.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class Foo(val id: String) {
    var description: String? = null
}

class Bar {
    var description: String? = null
    var extra: String? = null
}

@Konverter
interface FallbackMapper {
    fun map(source: Foo): Bar
}
                """.trimIndent()
            )
        )

        val generated = compilation.generatedSourceFor("FallbackMapperKonverter.kt")

        assertTrue("also" in generated)
        assertTrue("description = source.description" in generated)
        assertFalse("extra =" in generated)
    }

    @Test
    fun `strict mode respects @Mapping on constructor property`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "explicit",
            ),
            code = SourceFile.kotlin(
                name = "StrictMappingOnConstructor.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Foo(val identifier: String)

class Bar(val id: String)

@Konverter
interface ConstructorMappedMapper {
    @Konvert(mappings = [Mapping(target = "id", source = "identifier")])
    fun map(source: Foo): Bar
}
                """.trimIndent()
            )
        )

        val generated = compilation.generatedSourceFor("ConstructorMappedMapperKonverter.kt")

        assertTrue("id = source.identifier" in generated)
        assertFalse("also" in generated)
    }


    @Test
    fun `auto mode fails if unmapped and ignore-unmapped=false`() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "all",
            ),
            code = SourceFile.kotlin(
                name = "AutoFails.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class Foo(val id: String)

class Bar(val id: String) {
    var extra: String? = null
}

@Konverter
interface AutoFailsMapper {
    fun map(source: Foo): Bar
}
                """.trimIndent()
            )
        )

        assertTrue(
            compilationResult.messages.contains("Missing mappings for properties: extra"),
            "Expected failure due to unmapped 'extra' property:\n${compilationResult.messages}"
        )
    }


    @Test
    fun `ignore mode with Mapping on constructor property does not trigger fallback`() {
        val (compilation, _) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "auto",
            ),
            code = SourceFile.kotlin(
                name = "IgnoreWithConstructorMapping.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Foo(val identifier: String)

class Bar(val id: String) {
    var extra: String? = null
}

@Konverter
interface IgnoreCtorMapper {
    @Konvert(mappings = [Mapping(target = "id", source = "identifier")])
    fun map(source: Foo): Bar
}
                """.trimIndent()
            )
        )

        val generated = compilation.generatedSourceFor("IgnoreCtorMapperKonverter.kt")

        assertTrue("id = source.identifier" in generated)
        assertFalse("also" in generated)
    }


    @Test
    fun `ignore mode with Mapping on non-constructor property triggers fallback`() {
        val (compilation, _) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "auto",
            ),
            code = SourceFile.kotlin(
                name = "IgnoreFallback.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Foo(val id: String) {
    var description: String? = null
}

class Bar(val id: String) {
    var description: String? = null
}

@Konverter
interface IgnoreTriggersAlsoMapper {
    @Konvert(mappings = [Mapping(target = "description", source = "description")])
    fun map(source: Foo): Bar
}
                """.trimIndent()
            )
        )

        val generated = compilation.generatedSourceFor("IgnoreTriggersAlsoMapperKonverter.kt")

        assertTrue("description = source.description" in generated)
        assertTrue("also" in generated)
    }

    @Test
    fun `strict mode fails if explicit mapping source does not exist`() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "explicit",
            ),
            code = SourceFile.kotlin(
                name = "StrictFailsMissingSource.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Foo(val id: String)

class Bar(val id: String) {
    var missingSource: String? = null
}

@Konverter
interface StrictMissingSourceMapper {
    @Konvert(mappings = [
        Mapping(target = "missingSource", source = "notExistingProperty")
    ])
    fun map(source: Foo): Bar
}
                """.trimIndent()
            )
        )

        assertTrue(
            compilationResult.messages.contains("PropertyMappingNotExistingException"),
            "Expected failure due to missing source property:\n${compilationResult.messages}"
        )
    }

    @Test
    fun `strict mode maps only explicitly listed non-constructor property when multiple exist`() {
        val (compilation, _) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            options = mapOf(
                "konvert.non-constructor-properties-mapping" to "explicit",
            ),
            code = SourceFile.kotlin(
                name = "StrictMultipleNonConstructorProperties.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Foo(val id: String) {
    var description: String? = null
    var note: String? = null
}

class Bar(val id: String) {
    var description: String? = null
    var note: String? = null
    var extra: String? = null
}

@Konverter
interface StrictMultiplePropertiesMapper {
    @Konvert(
        mappings = [
            Mapping(target = "description", source = "description")
        ]
    )
    fun map(source: Foo): Bar
}
                """.trimIndent()
            )
        )

        val generated = compilation.generatedSourceFor("StrictMultiplePropertiesMapperKonverter.kt")

        assertTrue("description = source.description" in generated)
        assertFalse("note =" in generated)
        assertFalse("extra =" in generated)
    }

}
