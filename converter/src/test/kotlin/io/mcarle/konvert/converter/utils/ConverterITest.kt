package io.mcarle.konvert.converter.utils

import com.tschuchort.compiletesting.JvmCompilationResult
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

    /**
     * Enables logging of generated code
     */
    private val log = true

    private fun executeTest(
        sourceVariables: List<VariableNameToTypeNamePair>,
        targetVariables: List<VariableNameToTypeNamePair>,
        converter: TypeConverter,
        expectedResultCode: KotlinCompilation.ExitCode,
        additionalCode: List<SourceFile>,
        additionalConverter: Array<TypeConverter>,
        verification: (VerificationData) -> Unit,
    ): Pair<KotlinCompilation, JvmCompilationResult> {
        val sourceCode = generateSourceCode(sourceVariables)
        val targetCode = generateTargetCode(targetVariables)
        val mapperCode = generateMapper()

        val compilation = compileWith(
            enabledConverters = enabled(converter, *additionalConverter).toList(),
            expectResultCode = expectedResultCode,
            code = (listOf(sourceCode, targetCode, mapperCode) + additionalCode).toTypedArray()
        )

        val generatedMapperCode = try {
            compilation.first.generatedSourceFor("${mapperClassName}Konverter.kt")
        } catch (e: Exception) {
            if (compilation.second.exitCode == KotlinCompilation.ExitCode.OK) {
                throw e
            }
            null
        }
        if (log && generatedMapperCode != null) {
            println(generatedMapperCode)
        }

        if (compilation.second.exitCode == KotlinCompilation.ExitCode.OK) {

            val compilationResult = compilation.second

            val mapperKClass = compilationResult.classLoader.loadClass("${mapperClassName}Impl").kotlin

            verification(VerificationData(
                generatedCode = generatedMapperCode!!,
                converter = converter,
                sourceVariables = sourceVariables,
                targetVariables = targetVariables,
                mapperInstance = mapperKClass.objectInstance!!,
                mapperFunction = mapperKClass.members.first { it.name == mapperFunctionName },
                sourceKClass = compilationResult.classLoader.loadClass(sourceClassName).kotlin,
                targetKClass = compilationResult.classLoader.loadClass(targetClassName).kotlin
            ))
        }

        return compilation
    }

    protected fun executeTest(
        typeNamePairs: List<SourceToTargetTypeNamePair>,
        converter: TypeConverter,
        expectedResultCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalCode: List<SourceFile> = emptyList() ,
        additionalConverter: Array<TypeConverter> = emptyArray(),
        verification: (VerificationData) -> Unit = this::verify
    ): Pair<KotlinCompilation, JvmCompilationResult> {
        val sourceTypeNamePairs = typeNamePairs.mapIndexed { index, pair -> "test$index" to pair.first }
        val targetTypeNamePairs = typeNamePairs.mapIndexed { index, pair -> "test$index" to pair.second }
        return executeTest(
            sourceVariables = sourceTypeNamePairs,
            targetVariables = targetTypeNamePairs,
            converter = converter,
            expectedResultCode = expectedResultCode,
            additionalCode = additionalCode,
            additionalConverter = additionalConverter,
            verification = verification
        )
    }

    protected fun executeTest(
        sourceTypeName: String,
        targetTypeName: String,
        converter: TypeConverter,
        expectedResultCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
        additionalCode: List<SourceFile> = emptyList(),
        additionalConverter: Array<TypeConverter> = emptyArray(),
        verification: (VerificationData) -> Unit = this::verify
    ): Pair<KotlinCompilation, JvmCompilationResult> {
        return executeTest(
            typeNamePairs = listOf(sourceTypeName to targetTypeName),
            converter = converter,
            expectedResultCode = expectedResultCode,
            additionalCode = additionalCode,
            additionalConverter = additionalConverter,
            verification = verification
        )
    }

    private fun generateSourceCode(params: List<Pair<String, String>>) = SourceFile.kotlin(
        name = "$sourceClassName.kt",
        contents =
        """
class $sourceClassName(
    ${params.joinToString(",\n") { "val ${it.first}: ${it.second}" }}
)
        """.trimIndent()
    )

    private fun generateTargetCode(params: List<Pair<String, String>>) = SourceFile.kotlin(
        name = "$targetClassName.kt",
        contents =
        """
class $targetClassName(
    ${params.joinToString(",\n") { "val ${it.first}: ${it.second}" }}
)
        """.trimIndent()
    )

    private fun generateMapper() = SourceFile.kotlin(
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

    open fun verify(verificationData: VerificationData) {}
}

data class VerificationData(
    val generatedCode: String,
    val converter: TypeConverter,
    val sourceVariables: List<VariableNameToTypeNamePair>,
    val targetVariables: List<VariableNameToTypeNamePair>,
    val mapperInstance: Any,
    val mapperFunction: KCallable<*>,
    val sourceKClass: KClass<*>,
    val targetKClass: KClass<*>
)
