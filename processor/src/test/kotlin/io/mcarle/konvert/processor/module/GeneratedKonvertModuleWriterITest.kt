package io.mcarle.konvert.processor.module

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCompilerApi::class)
class GeneratedKonvertModuleWriterITest : KonverterITest() {

    override var addGeneratedKonverterAnnotation = true
    override var generatedModuleSuffix = "4Test"

    @ParameterizedTest
    @ValueSource(strings = ["", "a.b"])
    fun generateKonvertToFile(packageName: String) {
        val codeFileName = packageName.replace(".", "/").let {
            (if (it.isNotEmpty()) "$it/" else it) + "TestCode.kt"
        }
        val packageCodeLine = if (packageName.isNotEmpty()) "package $packageName" else ""
        val expectedPackagePrefix = packageName + if (packageName.isNotEmpty()) "." else ""
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = codeFileName,
                contents =
                """
$packageCodeLine
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(val property: String)
class TargetClass(val property: String)

                """.trimIndent()
            )
        )
        val moduleContent = compilation.generatedSourceFor("GeneratedModule.kt")
        println(moduleContent)

        assertContentEquals(
            """
            package generated.io.mcarle.konvert

            import io.mcarle.konvert.api.GeneratedKonvertModule

            @GeneratedKonvertModule(
              konverterFQN = [],
              konvertToFQN = ["${expectedPackagePrefix}toTargetClass"],
              konvertFromFQN = [],
            )
            public interface GeneratedModule4Test
            """.trimIndent(),
            moduleContent
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a.b"])
    fun generateKonvertFromFile(packageName: String) {
        val codeFileName = packageName.replace(".", "/").let {
            (if (it.isNotEmpty()) "$it/" else it) + "TestCode.kt"
        }
        val packageCodeLine = if (packageName.isNotEmpty()) "package $packageName" else ""
        val expectedPackagePrefix = packageName + if (packageName.isNotEmpty()) "." else ""
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = codeFileName,
                contents =
                """
$packageCodeLine
import io.mcarle.konvert.api.KonvertFrom

class SourceClass(val property: String)
@KonvertFrom(SourceClass::class)
class TargetClass(val property: String) {
    companion object
}

                """.trimIndent()
            )
        )
        val moduleContent = compilation.generatedSourceFor("GeneratedModule.kt")
        println(moduleContent)

        assertContentEquals(
            """
            package generated.io.mcarle.konvert

            import io.mcarle.konvert.api.GeneratedKonvertModule

            @GeneratedKonvertModule(
              konverterFQN = [],
              konvertToFQN = [],
              konvertFromFQN = ["${expectedPackagePrefix}fromSourceClass"],
            )
            public interface GeneratedModule4Test
            """.trimIndent(),
            moduleContent
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a.b"])
    fun generateKonvertFile(packageName: String) {
        val codeFileName = packageName.replace(".", "/").let {
            (if (it.isNotEmpty()) "$it/" else it) + "TestCode.kt"
        }
        val packageCodeLine = if (packageName.isNotEmpty()) "package $packageName" else ""
        val expectedPackagePrefix = packageName + if (packageName.isNotEmpty()) "." else ""
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = codeFileName,
                contents =
                """
$packageCodeLine
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val moduleContent = compilation.generatedSourceFor("GeneratedModule.kt")
        println(moduleContent)

        assertContentEquals(
            """
            package generated.io.mcarle.konvert

            import io.mcarle.konvert.api.GeneratedKonvertModule

            @GeneratedKonvertModule(
              konverterFQN = ["${expectedPackagePrefix}MapperImpl.toTarget"],
              konvertToFQN = [],
              konvertFromFQN = [],
            )
            public interface GeneratedModule4Test
            """.trimIndent(),
            moduleContent
        )
    }

    @Test
    fun addFqnForKonverterWithExistingCode() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass = TargetClass(source.property)
}
                """.trimIndent()
            )
        )
        val moduleContent = compilation.generatedSourceFor("GeneratedModule.kt")
        println(moduleContent)

        assertContentEquals(
            """
            package generated.io.mcarle.konvert

            import io.mcarle.konvert.api.GeneratedKonvertModule

            @GeneratedKonvertModule(
              konverterFQN = ["MapperImpl.toTarget"],
              konvertToFQN = [],
              konvertFromFQN = [],
            )
            public interface GeneratedModule4Test
            """.trimIndent(),
            moduleContent
        )
    }

    @Test
    fun addFqnForKonverterWithInheritedImplementedFunctions() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String)

interface IMapper {
    fun toTarget(source: SourceClass): TargetClass = TargetClass(source.property)
}

@Konverter
interface Mapper: IMapper {
}
                """.trimIndent()
            )
        )
        val moduleContent = compilation.generatedSourceFor("GeneratedModule.kt")
        println(moduleContent)

        assertContentEquals(
            """
            package generated.io.mcarle.konvert

            import io.mcarle.konvert.api.GeneratedKonvertModule

            @GeneratedKonvertModule(
              konverterFQN = ["MapperImpl.toTarget"],
              konvertToFQN = [],
              konvertFromFQN = [],
            )
            public interface GeneratedModule4Test
            """.trimIndent(),
            moduleContent
        )
    }

    @Test
    fun doNotAddFqnForKonverterFunctionsWithMultipleParameters() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String, val other: Int)

@Konverter
interface Mapper {
    fun toTarget(@Konverter.Source source: SourceClass, other: Int): TargetClass
}
                """.trimIndent()
            )
        )
        assertThrows<IllegalArgumentException> {
            compilation.generatedSourceFor("GeneratedModule.kt").also { println(it) }
        }
    }

}
