package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class KonverterITest {
    @TempDir
    protected lateinit var temporaryFolder: File

    fun compileWith(enabledConverters: List<TypeConverter>, vararg code: SourceFile): Pair<KotlinCompilation, KotlinCompilation.Result> {
        return compileWith(enabledConverters, emptyList(), *code)
    }

    fun compileWith(
        enabledConverters: List<TypeConverter>,
        otherConverters: List<TypeConverter>,
        vararg code: SourceFile
    ): Pair<KotlinCompilation, KotlinCompilation.Result> {
        return compileWith(enabledConverters, otherConverters, true, emptyMap(), *code)
    }

    fun compileWith(
        enabledConverters: List<TypeConverter>,
        otherConverters: List<TypeConverter>,
        expectSuccess: Boolean,
        vararg code: SourceFile
    ): Pair<KotlinCompilation, KotlinCompilation.Result> {
        return compileWith(enabledConverters, otherConverters, expectSuccess, emptyMap(), *code)
    }

    fun compileWith(
        enabledConverters: List<TypeConverter>,
        otherConverters: List<TypeConverter>,
        expectSuccess: Boolean,
        options: Map<String, String>,
        vararg code: SourceFile
    ): Pair<KotlinCompilation, KotlinCompilation.Result> {
        TypeConverterRegistry.reinitConverterList(*enabled(*enabledConverters.toTypedArray()), *otherConverters.toTypedArray())

        return compile(expectSuccess, options, *code)
    }

    private fun enabled(vararg converter: TypeConverter): Array<out TypeConverter> {
        return converter.map {
            object : TypeConverter by it {
                override val enabledByDefault: Boolean = true
            }
        }.toTypedArray()
    }

    private fun compile(
        expectSuccess: Boolean,
        options: Map<String, String>,
        vararg sourceFiles: SourceFile
    ): Pair<KotlinCompilation, KotlinCompilation.Result> {
        val compilation = prepareCompilation(options, sourceFiles.toList())

        val result = compilation.compile()
        if (expectSuccess) {
            assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        }

        return compilation to result
    }

    private fun prepareCompilation(options: Map<String, String>, sourceFiles: List<SourceFile>) = KotlinCompilation()
        .apply {
            workingDir = temporaryFolder
            inheritClassPath = true
            symbolProcessorProviders = listOf(KonvertProcessorProvider())
            sources = sourceFiles
            verbose = false
            jvmTarget = JvmTarget.JVM_17.description
            kspArgs += options
            kspWithCompilation = true
        }

    protected fun assertSourceEquals(@Language("kotlin") expected: String, generatedCode: String) {
        assertEquals(
            expected.trimIndent(),
            generatedCode.trimIndent()
        )
    }
}
