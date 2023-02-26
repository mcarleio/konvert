package io.mcarle.lib.kmapper.processor.converter.annotated

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.mcarle.lib.kmapper.processor.KMapProcessorProvider
import io.mcarle.lib.kmapper.processor.TypeConverter
import io.mcarle.lib.kmapper.processor.TypeConverterRegistry
import io.mcarle.lib.kmapper.processor.converter.generatedSourceFor
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

abstract class ConverterMapToITest {

    @TempDir
    protected lateinit var temporaryFolder: File
    protected val sourceClassName: String = "Xxx"
    protected val targetClassName: String = "Yyy"
    protected var expectedResultCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK

    /**
     * Enables logging of generated code
     */
    private val log = true

    protected open fun converterTest(converter: TypeConverter, sourceTypeName: String, targetTypeName: String) {
        val sourceCode = generateSourceCode(listOf("test" to sourceTypeName))
        val targetCode = generateTargetCode(listOf("test" to targetTypeName))

        val additionalCode = generateAdditionalCode()

        TypeConverterRegistry.reinitConverterList(converter, *additionalConverter())

        val compilation = if (additionalCode != null) {
            compile(sourceCode, targetCode, additionalCode)
        } else {
            compile(sourceCode, targetCode)
        }

        if (compilation != null) {
            val generatedMapperCode = compilation.first.generatedSourceFor("${sourceClassName}KMapExtensions.kt")
            if (log) {
                println(generatedMapperCode)
            }

            val compilationResult = compilation.second
//            val compilationResult = checkIfGeneratedMapperCompiles(compilation, generatedMapperCode + "\n" + (loadAdditionalCode(compilation) ?: ""))

            verifyMapper(
                sourceTypeName = sourceTypeName,
                targetTypeName = targetTypeName,
                mapperKClass = compilationResult.classLoader.loadClass(sourceClassName+"KMapExtensionsKt").kotlin, // ugly workaround to access generated member function
                sourceKClass = compilationResult.classLoader.loadClass(sourceClassName).kotlin,
                targetKClass = compilationResult.classLoader.loadClass(targetClassName).kotlin,
                classLoader = compilationResult.classLoader
            )

            validateGeneratedSourceCode(generatedMapperCode, sourceTypeName.endsWith("?"), targetTypeName.endsWith("?"))
        }
    }

    open fun generateAdditionalCode(): SourceFile? = null
    open fun additionalConverter(): Array<TypeConverter> = emptyArray()
    open fun loadAdditionalCode(compilation: KotlinCompilation): String? {
        return null
    }

    open fun validateGeneratedSourceCode(code: String, sourceTypeNullable: Boolean, targetTypeNullable: Boolean) {}

    open fun verifyMapper(
        sourceTypeName: String,
        targetTypeName: String,
        mapperKClass: KClass<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>,
        classLoader: ClassLoader
    ) {
    }

    fun generateSourceCode(params: List<Pair<String, String>>) = SourceFile.kotlin(
        name = "$sourceClassName.kt",
        contents =
        """
import io.mcarle.lib.kmapper.annotation.KMapTo

@KMapTo($targetClassName::class)
class $sourceClassName(
    ${params.joinToString(",\n") { "val ${it.first}: ${it.second}" }}
) {
//    @KMapTo($targetClassName::class)
    companion object
}
        """.trimIndent()
    )

    fun generateTargetCode(params: List<Pair<String, String>>) = SourceFile.kotlin(
        name = "$targetClassName.kt",
        contents =
        """
//import io.mcarle.lib.kmapper.annotation.KMapFrom

class $targetClassName(
    ${params.joinToString(",\n") { "val ${it.first}: ${it.second}" }}
) {
//    @KMapFrom($sourceClassName::class)
    companion object 
}
        """.trimIndent()
    )

    private fun compile(vararg sourceFiles: SourceFile): Pair<KotlinCompilation, KotlinCompilation.Result>? {
        val compilation = prepareCompilation(sourceFiles.toList())

        val result = compilation.compile()
        assertEquals(expected = expectedResultCode, actual = result.exitCode)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            return null
        }

        return compilation to result
    }
//
//    private fun checkIfGeneratedMapperCompiles(compilation: KotlinCompilation, code: String): KotlinCompilation.Result {
//        compilation.symbolProcessorProviders = emptyList()
//        compilation.sources += SourceFile.kotlin("${sourceClassName}KMapExtensions.kt", code)
//
//        val result = compilation.compile()
//        assertEquals(expected = KotlinCompilation.ExitCode.OK, actual = result.exitCode)
//        return result
//    }

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
