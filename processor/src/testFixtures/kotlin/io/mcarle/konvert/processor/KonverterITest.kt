package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.ADD_GENERATED_KONVERTER_ANNOTATION_OPTION
import io.mcarle.konvert.converter.api.config.ENFORCE_NOT_NULL_OPTION
import io.mcarle.konvert.converter.api.config.GENERATED_MODULE_SUFFIX_OPTION
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
abstract class KonverterITest {
    @TempDir
    protected lateinit var temporaryFolder: File

    protected open var addGeneratedKonverterAnnotation = false
    protected open var generatedModuleSuffix = ""
    protected open var enforceNotNull = false

    fun compileWith(
        enabledConverters: List<TypeConverter>,
        otherConverters: List<TypeConverter> = emptyList(),
        expectResultCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        options: Map<String, String> = emptyMap(),
        code: SourceFile,
        verbose: Boolean = false
    ): Pair<KotlinCompilation, JvmCompilationResult> {
        return compileWith(
            enabledConverters = enabledConverters,
            otherConverters = otherConverters,
            expectResultCode = expectResultCode,
            options = options,
            code = arrayOf(code),
            verbose = verbose
        )
    }

    fun compileWith(
        enabledConverters: List<TypeConverter>,
        otherConverters: List<TypeConverter> = emptyList(),
        expectResultCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        options: Map<String, String> = emptyMap(),
        code: Array<SourceFile>,
        verbose: Boolean = false
    ): Pair<KotlinCompilation, JvmCompilationResult> {
        TypeConverterRegistry.reinitConverterList(*enabled(*enabledConverters.toTypedArray()), *otherConverters.toTypedArray())

        return compile(expectResultCode, options, verbose, *code)
    }

    protected fun enabled(vararg converter: TypeConverter): Array<out TypeConverter> {
        return converter.map {
            object : TypeConverter by it {
                override val enabledByDefault: Boolean = true
            }
        }.toTypedArray()
    }

    private fun compile(
        expectResultCode: KotlinCompilation.ExitCode,
        options: Map<String, String>,
        verbose: Boolean,
        vararg sourceFiles: SourceFile
    ): Pair<KotlinCompilation, JvmCompilationResult> {
        val compilation = prepareCompilation(verbose, options, sourceFiles.toList())

        val result = compilation.compile()
        assertEquals(expectResultCode, result.exitCode)

        return compilation to result
    }

    private fun prepareCompilation(
        verboseCompilation: Boolean,
        options: Map<String, String>,
        sourceFiles: List<SourceFile>
    ) = KotlinCompilation().apply {
        workingDir = temporaryFolder
        inheritClassPath = true
        symbolProcessorProviders = mutableListOf(KonvertProcessorProvider())
        sources = sourceFiles
        verbose = verboseCompilation
        languageVersion = "1.9"
        jvmTarget = JvmTarget.JVM_17.description
        kspProcessorOptions += options.toMutableMap().apply {
            putIfAbsent(ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key, "$addGeneratedKonverterAnnotation")
            putIfAbsent(GENERATED_MODULE_SUFFIX_OPTION.key, generatedModuleSuffix)
            putIfAbsent(ENFORCE_NOT_NULL_OPTION.key, "$enforceNotNull")
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
