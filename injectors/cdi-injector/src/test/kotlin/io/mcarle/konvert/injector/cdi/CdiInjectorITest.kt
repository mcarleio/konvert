package io.mcarle.konvert.injector.cdi

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertContains

@OptIn(ExperimentalCompilerApi::class)
class CdiInjectorITest : KonverterITest() {


    @ParameterizedTest
    @ValueSource(strings = ["ApplicationScoped", "RequestScoped", "SessionScoped"])
    fun scoped(annotationName: String) {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.cdi.K$annotationName

@Konverter
@K$annotationName
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass): TargetClass
}

class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "import jakarta.enterprise.context.$annotationName")
        assertContains(mapperCode, "@$annotationName")
    }

}
