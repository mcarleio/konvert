package io.mcarle.konvert.processor.konvert

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalCompilerApi::class)
class SameTypeMappingITest : KonverterITest() {

    @Test
    fun sameTypeWithAdditionalParametersConstructsNewInstance() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class Entity(val id: Int, val name: String, val active: Boolean)

@Konverter
interface Mapper {
    fun clone(@Konverter.Source entity: Entity, name: String, active: Boolean): Entity
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import kotlin.Boolean
            import kotlin.String

            public object MapperImpl : Mapper {
              override fun clone(
                entity: Entity,
                name: String,
                active: Boolean,
              ): Entity = Entity(
                id = entity.id,
                name = name,
                active = active
              )
            }
            """.trimIndent(),
            mapperCode
        )

        val mapperClass = result.classLoader.loadClass("MapperImpl")
        val entityClass = result.classLoader.loadClass("Entity")
        val mapper = mapperClass.kotlin.objectInstance!!
        val source = entityClass.constructors.first().newInstance(1, "old", false)
        val clone = mapperClass.methods.first { it.name == "clone" }.invoke(mapper, source, "new", true)

        assertNotSame(source, clone)
        assertEquals(entityClass.constructors.first().newInstance(1, "new", true), clone)
    }

    @Test
    fun sameTypeWithRenameAndIgnoreMappingConstructsNewInstance() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class Entity(val id: Int, val name: String, val alias: String, val note: String? = null)

@Konverter
interface Mapper {
    @Konvert(mappings = [
        Mapping(target = "alias", source = "name"),
        Mapping(target = "note", ignore = true)
    ])
    fun swap(entity: Entity): Entity
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "Entity(")
        assertContains(mapperCode, "alias = entity.name")
        assertContains(mapperCode, "name = entity.name")
        assertContains(mapperCode, "id = entity.id")
        assertEquals(false, mapperCode.contains("note ="))
        assertEquals(false, mapperCode.contains("= entity\n"))
    }

    @Test
    fun plainSameTypeKeepsIdentity() {
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class Entity(val id: Int)

@Konverter
interface Mapper {
    fun copy(entity: Entity): Entity
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun copy(entity: Entity): Entity = entity
            }
            """.trimIndent(),
            mapperCode
        )

        val mapperClass = result.classLoader.loadClass("MapperImpl")
        val mapper = mapperClass.kotlin.objectInstance!!
        val source = result.classLoader.loadClass("Entity").constructors.first().newInstance(1)
        assertSame(source, mapperClass.methods.first { it.name == "copy" }.invoke(mapper, source))
    }

    @Test
    fun nestedSameTypePropertyKeepsIdentity() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class Address(val street: String)
class Person(val name: String, val address: Address)
class PersonDto(val name: String, val address: Address)

@Konverter
interface Mapper {
    fun toDto(@Konverter.Source person: Person, name: String): PersonDto
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "address = person.address")
        assertContains(mapperCode, "name = name")
    }

    @Test
    fun sameTypeWithAdditionalParameterFailsWhenTargetCannotBeConstructed() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

interface Shape {
    val name: String
}

@Konverter
interface Mapper {
    fun rename(@Konverter.Source shape: Shape, name: String): Shape
}
                """.trimIndent()
            )
        )

        assertContains(result.messages, "No constructor for Shape matching the available properties")
    }
}
