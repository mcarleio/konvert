package io.mcarle.konvert.processor.codegen

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION
import io.mcarle.konvert.converter.api.config.NonConstructorPropertiesMapping
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.exceptions.NotAssignableTargetException
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertContains

@OptIn(ExperimentalCompilerApi::class)
class NonPublicSetterITest : KonverterITest() {

    private fun entityCode(mapperBody: String) = SourceFile.kotlin(
        name = "TestCode.kt",
        contents =
            """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Source(val id: Long, val version: Int, val name: String)

abstract class BaseEntity {
    var id: Long? = null
        protected set
}

class Entity : BaseEntity() {
    var version: Int = 0
        private set
    var name: String = ""
}

@Konverter
interface Mapper {
$mapperBody
}
            """.trimIndent()
    )

    @ParameterizedTest
    @EnumSource(value = NonConstructorPropertiesMapping::class, names = ["IMPLICIT", "AUTO", "ALL"])
    fun `a var with a protected or private setter is not assigned from a same-named source property`(
        mode: NonConstructorPropertiesMapping
    ) {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(NON_CONSTRUCTOR_PROPERTIES_MAPPING_OPTION.key to mode.name),
            code = entityCode("    fun toEntity(source: Source): Entity")
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

    @Test
    fun `an ignored var with a non-public setter is accepted`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = entityCode(
                """
    @Konvert(mappings = [Mapping(target = "id", ignore = true)])
    fun toEntity(source: Source): Entity
                """.trimIndent()
            )
        )

        assertContains(compilation.generatedSourceFor("MapperKonverter.kt"), "entity.name = source.name")
    }

    @Test
    fun `an explicit mapping onto a var with a non-public setter is not assignable`() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = entityCode(
                """
    @Konvert(mappings = [Mapping(source = "version", target = "id")])
    fun toEntity(source: Source): Entity
                """.trimIndent()
            )
        )

        assertContains(result.messages, NotAssignableTargetException::class.qualifiedName!!)
        assertContains(result.messages, "`id`")
    }
}
