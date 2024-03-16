package io.mcarle.konvert.injector.koin

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.KONVERTER_GENERATE_CLASS_OPTION
import io.mcarle.konvert.injector.anvil.config.DEFAULT_INJECTION_METHOD_OPTION
import io.mcarle.konvert.injector.anvil.config.DEFAULT_SCOPE_OPTION
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

@OptIn(ExperimentalCompilerApi::class)
class AnvilInjectorITest : KonverterITest() {

    @Test
    fun contributesBinding() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(KONVERTER_GENERATE_CLASS_OPTION.key to "true"),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents = """
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

        assertSourceEquals(
            expected = """
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import kotlin.Unit

@ContributesBinding(
  scope = AppScope::class,
  boundType = Unit::class,
  replaces = arrayOf(),
  priority = ContributesBinding.Priority.NORMAL,
  ignoreQualifier = false,
)
public class MapperImpl : Mapper {
  @Inject
  public constructor()

  override fun toTarget(source: SourceClass): TargetClass = TargetClass(
    property = source.property
  )
}
            """.trimIndent(),
            generatedCode = mapperCode
        )
    }

    @Test
    fun contributesBindingAsSingleton() {
        val (compilation) = compileWith(
            listOf(element = SameTypeConverter()),
            options = mapOf(KONVERTER_GENERATE_CLASS_OPTION.key to "true"),
            code = SourceFile.kotlin(
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
        assertSourceEquals(
            """
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Unit

@ContributesBinding(
  scope = AppScope::class,
  boundType = Unit::class,
  replaces = arrayOf(),
  priority = ContributesBinding.Priority.NORMAL,
  ignoreQualifier = false,
)
@Singleton
public class MapperImpl : Mapper {
  @Inject
  public constructor()

  override fun toTarget(source: SourceClass): TargetClass = TargetClass(
    property = source.property
  )
}
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun contributesMultibinding() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(KONVERTER_GENERATE_CLASS_OPTION.key to "true"),
            code = SourceFile.kotlin(
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
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(KONVERTER_GENERATE_CLASS_OPTION.key to "true"),
            code = SourceFile.kotlin(
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

        assertSourceEquals(
            """
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject
import javax.inject.Named
import kotlin.Unit

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Unit::class,
  replaces = arrayOf(),
  ignoreQualifier = false,
)
@Named(`value` = "test")
public class MapperImpl : Mapper {
  @Inject
  public constructor()

  override fun toTarget(source: SourceClass): TargetClass = TargetClass(
    property = source.property
  )
}
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun contributesMultibindingWithCustomQualifier() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(KONVERTER_GENERATE_CLASS_OPTION.key to "true"),
            code = SourceFile.kotlin(
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

        assertSourceEquals(
            """
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject
import kotlin.Unit

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Unit::class,
  replaces = arrayOf(),
  ignoreQualifier = false,
)
@CustomQualifier
public class MapperImpl : Mapper {
  @Inject
  public constructor()

  override fun toTarget(source: SourceClass): TargetClass = TargetClass(
    property = source.property
  )
}
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun contributesMultibindingWithStringMapKey() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(KONVERTER_GENERATE_CLASS_OPTION.key to "true"),
            code = SourceFile.kotlin(
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

        assertSourceEquals(
            """
import com.squareup.anvil.annotations.ContributesMultibinding
import dagger.multibindings.StringKey
import javax.inject.Inject
import kotlin.Unit

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Unit::class,
  replaces = arrayOf(),
  ignoreQualifier = false,
)
@StringKey(`value` = "test")
public class MapperImpl : Mapper {
  @Inject
  public constructor()

  override fun toTarget(source: SourceClass): TargetClass = TargetClass(
    property = source.property
  )
}
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun contributesBindingByDefault() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(
                KONVERTER_GENERATE_CLASS_OPTION.key to "true",
                DEFAULT_INJECTION_METHOD_OPTION.key to "factory",
                DEFAULT_SCOPE_OPTION.key to "test.module.AppScope"
            ),
            code = SourceFile.kotlin(
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

        assertSourceEquals(
            """
package test.module

import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

@ContributesBinding(scope = AppScope::class)
public class MapperImpl : Mapper {
  @Inject
  public constructor()

  override fun toTarget(source: SourceClass): TargetClass = TargetClass(
    property = source.property
  )
}
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun contributesBindingAsSingletonByDefault() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(
                KONVERTER_GENERATE_CLASS_OPTION.key to "true",
                DEFAULT_INJECTION_METHOD_OPTION.key to "singleton",
                DEFAULT_SCOPE_OPTION.key to "test.module.AppScope"
            ),
            code = SourceFile.kotlin(
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

        assertSourceEquals(
            """
package test.module

import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject
import javax.inject.Singleton

@ContributesBinding(scope = AppScope::class)
@Singleton
public class MapperImpl : Mapper {
  @Inject
  public constructor()

  override fun toTarget(source: SourceClass): TargetClass = TargetClass(
    property = source.property
  )
}
        """.trimIndent(), mapperCode
        )
    }
}
