package io.mcarle.konvert.injector.koin

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.config.KONVERTER_GENERATE_CLASS
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.injector.anvil.config.DEFAULT_INJECTION_METHOD
import io.mcarle.konvert.injector.anvil.config.DEFAULT_SCOPE
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

class AnvilInjectorITest : KonverterITest() {

    @Test
    fun contributesBinding() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(KONVERTER_GENERATE_CLASS to "true"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.anvil.KContributesBinding
import com.squareup.anvil.annotations.ContributesBinding

abstract class AppScope private constructor()

@Konverter
@KContributesBinding(ContributesBinding(AppScope::class))
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

        assertContains(mapperCode, "import javax.inject.Inject")
        assertContains(mapperCode, "@Inject")
        assertContains(mapperCode, "public constructor()")

        assertContains(mapperCode, "com.squareup.anvil.annotations.ContributesBinding")
        assertContains(mapperCode, "@ContributesBinding")
        assertContains(mapperCode, "scope = AppScope::class")
    }

    @Test
    fun contributesBindingAsSingleton() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(KONVERTER_GENERATE_CLASS to "true"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.anvil.KContributesBinding
import io.mcarle.konvert.injector.anvil.KSingleton
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Singleton

abstract class AppScope private constructor()

@Konverter
@KContributesBinding(ContributesBinding(AppScope::class))
@KSingleton
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

        assertContains(mapperCode, "import javax.inject.Inject")
        assertContains(mapperCode, "@Inject")
        assertContains(mapperCode, "public constructor()")

        assertContains(mapperCode, "com.squareup.anvil.annotations.ContributesBinding")
        assertContains(mapperCode, "@ContributesBinding")
        assertContains(mapperCode, "scope = AppScope::class")

        assertContains(mapperCode, "javax.inject.Singleton")
        assertContains(mapperCode, "@Singleton")
    }

    @Test
    fun contributesMultibinding() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(KONVERTER_GENERATE_CLASS to "true"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.anvil.KContributesMultibinding
import com.squareup.anvil.annotations.ContributesMultibinding

abstract class AppScope private constructor()

@Konverter
@KContributesMultibinding(ContributesMultibinding(AppScope::class))
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

        assertContains(mapperCode, "import javax.inject.Inject")
        assertContains(mapperCode, "@Inject")
        assertContains(mapperCode, "public constructor()")

        assertContains(mapperCode, "com.squareup.anvil.annotations.ContributesMultibinding")
        assertContains(mapperCode, "@ContributesMultibinding")
        assertContains(mapperCode, "scope = AppScope::class")
    }

    @Test
    fun contributesMultibindingWithNamedQualifier() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(KONVERTER_GENERATE_CLASS to "true"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.anvil.KContributesMultibinding
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Named

abstract class AppScope private constructor()

@Konverter
@KContributesMultibinding(ContributesMultibinding(AppScope::class))
@Named("test")
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

        assertContains(mapperCode, "import javax.inject.Inject")
        assertContains(mapperCode, "@Inject")
        assertContains(mapperCode, "public constructor()")

        assertContains(mapperCode, "com.squareup.anvil.annotations.ContributesMultibinding")
        assertContains(mapperCode, "@ContributesMultibinding")
        assertContains(mapperCode, "scope = AppScope::class")

        assertContains(mapperCode, "import javax.inject.Named")
        assertContains(mapperCode, "@Named(`value` = \"test\")")
    }

    @Test
    fun contributesMultibindingWithCustomQualifier() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(KONVERTER_GENERATE_CLASS to "true"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.anvil.KContributesMultibinding
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Qualifier

abstract class AppScope private constructor()

@Qualifier
annotation class CustomQualifier

@Konverter
@KContributesMultibinding(ContributesMultibinding(AppScope::class))
@CustomQualifier
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

        assertContains(mapperCode, "import javax.inject.Inject")
        assertContains(mapperCode, "@Inject")
        assertContains(mapperCode, "public constructor()")

        assertContains(mapperCode, "com.squareup.anvil.annotations.ContributesMultibinding")
        assertContains(mapperCode, "@ContributesMultibinding")
        assertContains(mapperCode, "scope = AppScope::class")

        assertContains(mapperCode, "@CustomQualifier")
    }

    @Test
    fun contributesMultibindingWithStringMapKey() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(KONVERTER_GENERATE_CLASS to "true"),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.injector.anvil.KContributesMultibinding
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.multibindings.StringKey

abstract class AppScope private constructor()

@Konverter
@KContributesMultibinding(ContributesMultibinding(AppScope::class))
@StringKey("test")
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

        assertContains(mapperCode, "import javax.inject.Inject")
        assertContains(mapperCode, "@Inject")
        assertContains(mapperCode, "public constructor()")

        assertContains(mapperCode, "com.squareup.anvil.annotations.ContributesMultibinding")
        assertContains(mapperCode, "@ContributesMultibinding")
        assertContains(mapperCode, "scope = AppScope::class")

        assertContains(mapperCode, "dagger.multibindings.StringKey")
        assertContains(mapperCode, "@StringKey(`value` = \"test\")")
    }

    @Test
    fun contributesBindingByDefault() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(
                KONVERTER_GENERATE_CLASS to "true",
                DEFAULT_INJECTION_METHOD to "factory",
                DEFAULT_SCOPE to "test.module.AppScope"
            ),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
package test.module

import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert

abstract class AppScope private constructor()

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

        assertContains(mapperCode, "import javax.inject.Inject")
        assertContains(mapperCode, "@Inject")
        assertContains(mapperCode, "public constructor()")

        assertContains(mapperCode, "com.squareup.anvil.annotations.ContributesBinding")
        assertContains(mapperCode, "@ContributesBinding")
        assertContains(mapperCode, "scope = AppScope::class")
    }

    @Test
    fun contributesBindingAsSingletonByDefault() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(
                KONVERTER_GENERATE_CLASS to "true",
                DEFAULT_INJECTION_METHOD to "singleton",
                DEFAULT_SCOPE to "test.module.AppScope"
            ),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
package test.module

import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert

abstract class AppScope private constructor()

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

        assertContains(mapperCode, "import javax.inject.Inject")
        assertContains(mapperCode, "@Inject")
        assertContains(mapperCode, "public constructor()")

        assertContains(mapperCode, "com.squareup.anvil.annotations.ContributesBinding")
        assertContains(mapperCode, "@ContributesBinding")
        assertContains(mapperCode, "scope = AppScope::class")

        assertContains(mapperCode, "javax.inject.Singleton")
        assertContains(mapperCode, "@Singleton")
    }
}
