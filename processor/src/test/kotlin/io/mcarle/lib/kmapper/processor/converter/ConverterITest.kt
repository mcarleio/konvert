package io.mcarle.lib.kmapper.processor.converter

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.mcarle.lib.kmapper.processor.KMapperProcessorProvider
import io.mcarle.lib.kmapper.processor.TypeConverterRegistry
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

abstract class ConverterITest {

    @TempDir
    protected lateinit var temporaryFolder: File
    private val sourceClassName: String = "Xxx"
    private val targetClassName: String = "Yyy"
    private val mapperClassName: String = "FooMapper"
    private val mapperFunctionName: String = "to$targetClassName"
    private val mapperFunctionParamName: String = "it"
    protected var expectedResultCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK

    /**
     * Enables logging of generated code
     */
    private val log = true

    protected open fun converterTest(converter: TypeConverter, sourceTypeName: String, targetTypeName: String) {
        val sourceCode = generateSourceCode(listOf("test" to sourceTypeName))
        val targetCode = generateTargetCode(listOf("test" to targetTypeName))
        val mapperCode = generateMapper()

        val additionalCode = generateAdditionalCode()

        TypeConverterRegistry.reinitConverterList(converter, *additionalConverter())

        val compilation = if (additionalCode != null) {
            compile(sourceCode, targetCode, mapperCode, additionalCode)
        } else {
            compile(sourceCode, targetCode, mapperCode)
        }

        if (compilation != null) {
            val generatedMapperCode = compilation.generatedSourceFor("${mapperClassName}Impl.kt")
            if (log) {
                println(generatedMapperCode)
            }

            val compilationResult = checkIfGeneratedMapperCompiles(compilation, generatedMapperCode + "\n" + (loadAdditionalCode(compilation) ?: ""))

            val mapperKClass = compilationResult.classLoader.loadClass("${mapperClassName}Impl").kotlin

            verifyMapper(
                sourceTypeName = sourceTypeName,
                targetTypeName = targetTypeName,
                mapperInstance = mapperKClass.objectInstance!!,
                mapperFunction = mapperKClass.members.first { it.name == mapperFunctionName },
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
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>,
        classLoader: ClassLoader
    ) {
        verifyMapper(sourceTypeName, targetTypeName, mapperInstance, mapperFunction, sourceKClass, targetKClass)
    }
    open fun verifyMapper(
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
    }

    fun generateSourceCode(params: List<Pair<String, String>>) = SourceFile.kotlin(
        name = "$sourceClassName.kt",
        contents =
        """
class $sourceClassName(
    ${params.joinToString(",\n") { "val ${it.first}: ${it.second}" }}
)
        """.trimIndent()
    )

    fun generateTargetCode(params: List<Pair<String, String>>) = SourceFile.kotlin(
        name = "$targetClassName.kt",
        contents =
        """
class $targetClassName(
    ${params.joinToString(",\n") { "val ${it.first}: ${it.second}" }}
)
        """.trimIndent()
    )

    fun generateMapper() = SourceFile.kotlin(
        name = "$mapperClassName.kt",
        contents =
        """
import io.mcarle.lib.kmapper.annotation.KMapper
import io.mcarle.lib.kmapper.annotation.KMapping
import io.mcarle.lib.kmapper.annotation.KMap

@KMapper
interface $mapperClassName {
    @KMapping
    fun $mapperFunctionName($mapperFunctionParamName: $sourceClassName): $targetClassName
}
        """
    )


    private fun compile(vararg sourceFiles: SourceFile): KotlinCompilation? {
        val compilation = prepareCompilation(sourceFiles.toList())

        val result = compilation.compile()
        assertEquals(expected = expectedResultCode, actual = result.exitCode)

        if (result.exitCode != KotlinCompilation.ExitCode.OK) {
            return null
        }

        return compilation
    }

    private fun checkIfGeneratedMapperCompiles(compilation: KotlinCompilation, code: String): KotlinCompilation.Result {
        compilation.symbolProcessorProviders = emptyList()
        compilation.sources += SourceFile.kotlin("${mapperClassName}Impl.kt", code)

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

    protected fun assertSourceEquals(@Language("kotlin") expected: String, generatedCode: String) {
        assertEquals(
            expected.trimIndent(),
            generatedCode.trimIndent()
        )
    }

}
