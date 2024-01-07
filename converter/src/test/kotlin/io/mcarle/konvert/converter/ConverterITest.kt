package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.api.TypeConverter
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

@OptIn(ExperimentalCompilerApi::class)
abstract class ConverterITest : KonverterITest() {

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

    protected fun converterTest(converter: TypeConverter, sourceTypeName: String, targetTypeName: String) {
        val sourceCode = generateSourceCode(listOf("test" to sourceTypeName))
        val targetCode = generateTargetCode(listOf("test" to targetTypeName))
        val mapperCode = generateMapper()

        val additionalCode = generateAdditionalCode()

        val compilation = compileWith(
            enabledConverters = enabled(converter, *additionalConverter()).toList(),
            expectResultCode = this.expectedResultCode,
            code = (listOf(sourceCode, targetCode, mapperCode) + additionalCode).toTypedArray()
        )

        if (compilation.second.exitCode == KotlinCompilation.ExitCode.OK) {
            val generatedMapperCode = compilation.first.generatedSourceFor("${mapperClassName}Konverter.kt")
            if (log) {
                println(generatedMapperCode)
            }

//            val compilationResult = checkIfGeneratedMapperCompiles(compilation, generatedMapperCode + "\n" + (loadAdditionalCode(compilation) ?: ""))
            val compilationResult = compilation.second

            val mapperKClass = compilationResult.classLoader.loadClass("${mapperClassName}Impl").kotlin

            verifyMapper(
                converter = converter,
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

    private fun enabled(vararg converter: TypeConverter): Array<out TypeConverter> {
        return converter.map {
            object : TypeConverter by it {
                override val enabledByDefault: Boolean = true
            }
        }.toTypedArray()
    }

    open fun generateAdditionalCode(): List<SourceFile> = emptyList()
    open fun additionalConverter(): Array<TypeConverter> = emptyArray()
    open fun loadAdditionalCode(compilation: KotlinCompilation): String? {
        return null
    }

    open fun validateGeneratedSourceCode(code: String, sourceTypeNullable: Boolean, targetTypeNullable: Boolean) {}

    open fun verifyMapper(
        converter: TypeConverter,
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>,
        classLoader: ClassLoader
    ) {
        verifyMapper(converter, sourceTypeName, targetTypeName, mapperInstance, mapperFunction, sourceKClass, targetKClass)
    }

    open fun verifyMapper(
        converter: TypeConverter,
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
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

@Konverter
interface $mapperClassName {
    @Konvert
    fun $mapperFunctionName($mapperFunctionParamName: $sourceClassName): $targetClassName
}
        """
    )

}
