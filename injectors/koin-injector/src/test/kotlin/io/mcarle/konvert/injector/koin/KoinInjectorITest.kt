package io.mcarle.konvert.injector.koin

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.injector.koin.config.DEFAULT_INJECTION_METHOD
import io.mcarle.konvert.injector.koin.config.DEFAULT_SCOPE
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

class KoinInjectorITest : KonverterITest() {

    @Test
    fun single() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KSingle

@Konverter
@KSingle
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

        assertContains(mapperCode, "org.koin.core.`annotation`.Single")
        assertContains(mapperCode, "@Single")
    }

    @Test
    fun singleWithBinding() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KSingle
import org.koin.core.annotation.Single

@Konverter
@KSingle(Single(binds=[TestScope::class]))
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass): TargetClass
}

object TestScope

class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "org.koin.core.`annotation`.Single")
        assertContains(mapperCode, "@Single")
    }

    @Test
    fun factory() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KFactory

@Konverter
@KFactory
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

        assertContains(mapperCode, "org.koin.core.`annotation`.Factory")
        assertContains(mapperCode, "@Factory")
    }

    @Test
    fun factoryWithBinding() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KFactory
import org.koin.core.annotation.Factory

@Konverter
@KFactory(Factory(binds=[TestScope::class]))
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass): TargetClass
}

object TestScope

class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "org.koin.core.`annotation`.Factory")
        assertContains(mapperCode, "@Factory")
    }

    @Test
    fun singleNamed() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KSingle
import io.mcarle.konvert.injector.koin.KNamed
import org.koin.core.annotation.Named

@Konverter
@KSingle
@KNamed(Named("test_name"))
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

        assertContains(mapperCode, "org.koin.core.`annotation`.Single")
        assertContains(mapperCode, "@Single")
        assertContains(mapperCode, "org.koin.core.`annotation`.Named")
        assertContains(mapperCode, "@Named(value = \"test_name\")")
    }

    @Test
    fun factoryScoped() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KFactory
import io.mcarle.konvert.injector.koin.KScope
import io.mcarle.konvert.injector.koin.KScoped
import org.koin.core.annotation.Scope

@Konverter
@KFactory
@KScope(Scope(name = "scope_name"))
@KScoped
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

        assertContains(mapperCode, "org.koin.core.`annotation`.Factory")
        assertContains(mapperCode, "@Factory")
        assertContains(mapperCode, "org.koin.core.`annotation`.Scope")
        assertContains(mapperCode, "@Scope(name = \"scope_name\")")
        assertContains(mapperCode, "org.koin.core.`annotation`.Scoped")
        assertContains(mapperCode, "@Scoped")
    }

    @Test
    fun factoryScopedWithClass() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KFactory
import io.mcarle.konvert.injector.koin.KScope
import io.mcarle.konvert.injector.koin.KScoped
import org.koin.core.annotation.Scope

@Konverter
@KFactory
@KScope(Scope(value = TestScope::class))
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass): TargetClass
}

object TestScope

class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "org.koin.core.`annotation`.Factory")
        assertContains(mapperCode, "@Factory")
        assertContains(mapperCode, "org.koin.core.`annotation`.Scope")
        assertContains(mapperCode, "@Scope")
        assertContains(mapperCode, "`value` = TestScope::class")
    }

    @Test
    fun scopedWithBinding() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KFactory
import io.mcarle.konvert.injector.koin.KScope
import io.mcarle.konvert.injector.koin.KScoped
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Konverter
@KFactory
@KScope(Scope(name = "scope_name"))
@KScoped(Scoped(binds=[TestScope::class]))
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass): TargetClass
}

object TestScope

class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "org.koin.core.`annotation`.Factory")
        assertContains(mapperCode, "@Factory")
        assertContains(mapperCode, "org.koin.core.`annotation`.Scope")
        assertContains(mapperCode, "@Scope(name = \"scope_name\")")
        assertContains(mapperCode, "org.koin.core.`annotation`.Scoped")
        assertContains(mapperCode, "@Scoped")
    }

    @Test
    fun defaultFactory() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(DEFAULT_INJECTION_METHOD to "factory"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KFactory
import io.mcarle.konvert.injector.koin.KScope
import io.mcarle.konvert.injector.koin.KScoped

@Konverter
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

        assertContains(mapperCode, "org.koin.core.`annotation`.Factory")
        assertContains(mapperCode, "@Factory")
    }

    @Test
    fun defaultSingle() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(DEFAULT_INJECTION_METHOD to "single"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KFactory
import io.mcarle.konvert.injector.koin.KScope
import io.mcarle.konvert.injector.koin.KScoped

@Konverter
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

        assertContains(mapperCode, "org.koin.core.`annotation`.Single")
        assertContains(mapperCode, "@Single")
    }

    @Test
    fun defaultScopeWithClassParam() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(DEFAULT_INJECTION_METHOD to "scope", DEFAULT_SCOPE to "test.module.TestScope"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
package test.module

import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KFactory
import io.mcarle.konvert.injector.koin.KScope
import io.mcarle.konvert.injector.koin.KScoped

@Konverter
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass): TargetClass
}

object TestScope

class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "org.koin.core.`annotation`.Scope")
        assertContains(mapperCode, "@Scope")
        assertContains(mapperCode, "value = TestScope::class")
    }

    @Test
    fun defaultScopeWithStringParam() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(DEFAULT_INJECTION_METHOD to "scope", DEFAULT_SCOPE to "ScopeName"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
package test.module

import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.koin.KFactory
import io.mcarle.konvert.injector.koin.KScope
import io.mcarle.konvert.injector.koin.KScoped

@Konverter
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass): TargetClass
}

object TestScope

class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "org.koin.core.`annotation`.Scope")
        assertContains(mapperCode, "@Scope")
        assertContains(mapperCode, "name = \"ScopeName\"")
    }
}
