package io.mcarle.lib.kmapper.processor.converter

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.mcarle.lib.kmapper.processor.KMapProcessorProvider
import io.mcarle.lib.kmapper.converter.api.TypeConverter
import io.mcarle.lib.kmapper.converter.api.TypeConverterRegistry
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

abstract class ConverterITest {
    @TempDir
    protected lateinit var temporaryFolder: File

    fun compileWith(enabledConverters: List<TypeConverter>, vararg code: SourceFile): Pair<KotlinCompilation, KotlinCompilation.Result> {
        TypeConverterRegistry.reinitConverterList(*enabled(*enabledConverters.toTypedArray()))

        return compile(*code)
    }

    private fun enabled(vararg converter: TypeConverter): Array<out TypeConverter> {
        return converter.map {
            object : TypeConverter by it {
                override val enabledByDefault: Boolean = true
            }
        }.toTypedArray()
    }

    private fun compile(vararg sourceFiles: SourceFile): Pair<KotlinCompilation, KotlinCompilation.Result> {
        val compilation = prepareCompilation(sourceFiles.toList())

        val result = compilation.compile()
        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)

        return compilation to result
    }

    private fun prepareCompilation(sourceFiles: List<SourceFile>) = KotlinCompilation()
        .apply {
            workingDir = temporaryFolder
            inheritClassPath = true
            symbolProcessorProviders = listOf(KMapProcessorProvider())
            sources = sourceFiles
            verbose = false
            kspWithCompilation = true
        }

    protected fun assertSourceEquals(@Language("kotlin") expected: String, generatedCode: String) {
        assertEquals(
            expected.trimIndent(),
            generatedCode.trimIndent()
        )
    }
}