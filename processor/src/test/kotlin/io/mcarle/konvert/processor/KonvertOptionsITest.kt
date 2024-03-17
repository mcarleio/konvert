package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.IterableToListConverter
import io.mcarle.konvert.converter.SameTypeConverter
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class KonvertOptionsITest: KonverterITest() {

    @Test
    fun `konvert konverter use-reflection`() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Konfig

@Konverter
interface PersonMapper {
    fun toDto(person: Person): PersonDto
}

class Person(val age: String)
class PersonDto(val age: String)

@KonvertTo(GroupDto::class, options=[
    Konfig(key="konvert.konverter.use-reflection", value="true")
], mapFunctionName="toGroupWithReflection")
@KonvertTo(GroupDto::class, mapFunctionName="toGroupWithoutReflection")
class Group(val members: List<Person>)
class GroupDto(val members: List<PersonDto>)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("GroupKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.Konverter

            public fun Group.toGroupWithReflection(): GroupDto = GroupDto(
              members = members.map { Konverter.get<PersonMapper>().toDto(person = it) }
            )

            public fun Group.toGroupWithoutReflection(): GroupDto = GroupDto(
              members = members.map { PersonMapperImpl.toDto(person = it) }
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }
}
