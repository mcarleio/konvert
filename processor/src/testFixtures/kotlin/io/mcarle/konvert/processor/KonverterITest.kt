package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.ADD_GENERATED_KONVERTER_ANNOTATION_OPTION
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class KonverterITest {
    @TempDir
    protected lateinit var temporaryFolder: File

    protected open var addGeneratedKonverterAnnotation = false

    fun compileWith(
        enabledConverters: List<TypeConverter>,
        otherConverters: List<TypeConverter> = emptyList(),
        expectResultCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        options: Map<String, String> = emptyMap(),
        code: SourceFile
    ): Pair<KotlinCompilation, KotlinCompilation.Result> {
        return compileWith(
            enabledConverters = enabledConverters,
            otherConverters = otherConverters,
            expectResultCode = expectResultCode,
            options = options,
            code = arrayOf(code)
        )
    }

    fun compileWith(
        enabledConverters: List<TypeConverter>,
        otherConverters: List<TypeConverter> = emptyList(),
        expectResultCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        options: Map<String, String> = emptyMap(),
        code: Array<SourceFile>
    ): Pair<KotlinCompilation, KotlinCompilation.Result> {
        TypeConverterRegistry.reinitConverterList(*enabled(*enabledConverters.toTypedArray()), *otherConverters.toTypedArray())

        return compile(expectResultCode, options, *code)
    }

    private fun enabled(vararg converter: TypeConverter): Array<out TypeConverter> {
        return converter.map {
            object : TypeConverter by it {
                override val enabledByDefault: Boolean = true
            }
        }.toTypedArray()
    }

    private fun compile(
        expectResultCode: KotlinCompilation.ExitCode,
        options: Map<String, String>,
        vararg sourceFiles: SourceFile
    ): Pair<KotlinCompilation, KotlinCompilation.Result> {
        val compilation = prepareCompilation(options, sourceFiles.toList())

        val result = compilation.compile()
        assertEquals(expectResultCode, result.exitCode)

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
            kspArgs += options.toMutableMap().apply {
                putIfAbsent(ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key, "$addGeneratedKonverterAnnotation")
            }
            kspWithCompilation = true
        }

    protected fun assertSourceEquals(@Language("kotlin") expected: String, generatedCode: String) {
        assertEquals(
            expected.trimIndent(),
            generatedCode.trimIndent()
        )
    }
    protected fun assertContentEquals(expected: String, generatedCode: String) {
        assertEquals(
            expected.trimIndent(),
            generatedCode.trimIndent()
        )
    }
}
