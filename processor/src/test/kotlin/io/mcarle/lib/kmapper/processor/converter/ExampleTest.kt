package io.mcarle.lib.kmapper.processor.converter

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.mcarle.lib.kmapper.processor.KMapperProcessorProvider
import io.mcarle.lib.kmapper.processor.TypeConverterRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

class ExampleTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Test
    fun converterTest() {
        val code = SourceFile.kotlin(
            name = "Source.kt",
            contents =
            """
import io.mcarle.lib.kmapper.annotation.KMapper
import io.mcarle.lib.kmapper.annotation.KMapping
import io.mcarle.lib.kmapper.annotation.KMap

data class Name(
    val first: String,
    val last: String
)

data class Address(
    val street: String,
    val num: Int
)

data class PersonDto(
    val name: String,
    val address: Address
)

data class Person(
    val name: Name,
    val street: String,
    val streetNum: String
)

@KMapper
interface FooMapper {

    @KMapping(mappings= [
        KMap(target = "address", expression= "toAddress(person)"),
        KMap(target = "name", expression= "fromName(person.name)")
    ])
    fun toPersonDto(person: Person): PersonDto
    
    @KMapping(mappings= [
        KMap(target = "num", source = "streetNum")
    ])
    fun toAddress(person: Person): Address
    
    // fun name(dto: PersonDto) = Name(dto.name.split(" ")[0], dto.name.split(" ")[1]) 
    fun fromName(name: Name) = name.first + " " + name.last 
    
}
        """.trimIndent()
        )

        TypeConverterRegistry.reinitConverterList(*TypeConverterRegistry.defaultConverter.toTypedArray())

        val compilation = compile(code)

        val generatedMapperCode = compilation.generatedSourceFor("FooMapperImpl.kt")
        println(generatedMapperCode)

        val compilationResult = checkIfGeneratedMapperCompiles(compilation, generatedMapperCode)

        val mapperKClass = compilationResult.classLoader.loadClass("FooMapperImpl").kotlin
    }

    private fun compile(vararg sourceFiles: SourceFile): KotlinCompilation {
        val compilation = prepareCompilation(sourceFiles.toList())

        val result = compilation.compile()
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        return compilation
    }

    private fun checkIfGeneratedMapperCompiles(compilation: KotlinCompilation, code: String): KotlinCompilation.Result {
        compilation.symbolProcessorProviders = emptyList()
        compilation.sources += SourceFile.kotlin("FooMapperImpl.kt", code)

        val result = compilation.compile()
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
        return result
    }

    private fun prepareCompilation(sourceFiles: List<SourceFile>) = KotlinCompilation()
        .apply {
            workingDir = temporaryFolder
            inheritClassPath = true
            symbolProcessorProviders = listOf(KMapperProcessorProvider())
            sources = sourceFiles
            verbose = false
        }

}