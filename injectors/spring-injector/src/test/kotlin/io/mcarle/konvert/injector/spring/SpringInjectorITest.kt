package io.mcarle.konvert.injector.spring

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

class SpringInjectorITest : KonverterITest() {

    @Test
    fun componentAndScope() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.spring.KComponent
import io.mcarle.konvert.injector.spring.KScope
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode

@Konverter
@KComponent
@KScope(Scope("aa", scopeName = "bb", proxyMode = ScopedProxyMode.TARGET_CLASS))
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

        assertContains(mapperCode, "import org.springframework.stereotype.Component")
        assertContains(mapperCode, "@Component")
        assertContains(mapperCode, "import org.springframework.context.`annotation`.Scope")
        assertContains(mapperCode, "@Scope")
    }

}
