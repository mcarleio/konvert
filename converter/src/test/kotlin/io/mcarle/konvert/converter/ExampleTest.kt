package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.processor.KonvertProcessorProvider
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class ExampleTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Test
    fun converterTest() {
        val code = SourceFile.kotlin(
            name = "Source.kt",
            contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

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

@Konverter
interface FooMapper {

    @Konvert(mappings= [
        Mapping(target = "address", expression= "toAddress(person)"),
        Mapping(target = "name", expression= "fromName(person.name)")
    ])
    fun toPersonDto(person: Person): PersonDto

    @Konvert(mappings= [
        Mapping(target = "num", source = "streetNum", enable=["StringToIntConverter"])
    ])
    fun toAddress(person: Person): Address

    // fun name(dto: PersonDto) = Name(dto.name.split(" ")[0], dto.name.split(" ")[1])
    fun fromName(name: Name) = name.first + " " + name.last

}
        """.trimIndent()
        )

        TypeConverterRegistry.reinitConverterList(*TypeConverterRegistry.availableConverters.toTypedArray())

        val compilation = compile(code)

        val generatedMapperCode = compilation.generatedSourceFor("FooMapperKonverter.kt")
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

    private fun checkIfGeneratedMapperCompiles(compilation: KotlinCompilation, code: String): JvmCompilationResult {
        compilation.symbolProcessorProviders = mutableListOf()
        compilation.sources += SourceFile.kotlin("FooMapperKonverter.kt", code)

        val result = compilation.compile()
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
        return result
    }

    private fun prepareCompilation(sourceFiles: List<SourceFile>) = KotlinCompilation()
        .apply {
            workingDir = temporaryFolder
            inheritClassPath = true
            languageVersion = "1.9"
            symbolProcessorProviders = mutableListOf(KonvertProcessorProvider())
            jvmTarget = JvmTarget.JVM_17.description
            sources = sourceFiles
            verbose = false
        }

}
