package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.GeneratedKonverter
import io.mcarle.konvert.converter.IterableToIterableConverter
import io.mcarle.konvert.converter.IterableToSetConverter
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.ADD_GENERATED_KONVERTER_ANNOTATION_OPTION
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
class GeneratedKonverterITest : KonverterITest() {

    override var addGeneratedKonverterAnnotation = true

    @Test
    fun useGeneratedKonverterWithHighestPriority() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.processor.SomeTestClass
import io.mcarle.konvert.processor.SomeOtherTestClass

@KonvertTo(TargetClass::class)
class SourceClass(val property: SomeTestClass)
class TargetClass(val property: SomeOtherTestClass)

                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter
            import io.mcarle.konvert.processor.toSomeOtherTestClass

            @GeneratedKonverter(priority = 3_000)
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property.toSomeOtherTestClass()
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun useGeneratedKonverterInsteadOfIterableToIterableConverter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(IterableToIterableConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.processor.SomeTestClass
import io.mcarle.konvert.processor.SomeOtherTestClass

@KonvertTo(TargetClass::class)
class SourceClass(val property: List<SomeTestClass>)
class TargetClass(val property: List<SomeOtherTestClass>)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter
            import io.mcarle.konvert.processor.SomeTestMapperImpl

            @GeneratedKonverter(priority = 3_000)
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = SomeTestMapperImpl.toSomeOtherTestClasses(source = property)
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun useGeneratedKonverterInstanceInsteadOfIterableToIterableConverter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(IterableToIterableConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.processor.SomeTestClass
import io.mcarle.konvert.processor.SomeOtherTestClass

@KonvertTo(TargetClass::class)
class SourceClass(val property: List<SomeOtherTestClass>)
class TargetClass(val property: List<SomeTestClass>)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter
            import io.mcarle.konvert.processor.SomeSecondTestMapperImpl

            @GeneratedKonverter(priority = 3_000)
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = SomeSecondTestMapperImpl().fromSomeOtherTestClasses(source = property)
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun useIterableToIterableConverterInsteadOfGeneratedKonverterWhenNotExactMatch() {
        val (compilation) = compileWith(
            enabledConverters = listOf(IterableToSetConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.processor.SomeTestClass
import io.mcarle.konvert.processor.SomeOtherTestClass

@KonvertTo(TargetClass::class)
class SourceClass(val property: List<SomeTestClass>)
class TargetClass(val property: Set<SomeOtherTestClass>)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter
            import io.mcarle.konvert.processor.toSomeOtherTestClass

            @GeneratedKonverter(priority = 3_000)
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property.map { it.toSomeOtherTestClass() }.toSet()
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun generateGeneratedKonverterAnnotationForKonvertTo() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class, priority = 123)
class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter

            @GeneratedKonverter(priority = 123)
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun generateGeneratedKonverterAnnotationForKonvertFrom() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom

class SourceClass(val property: String)

@KonvertFrom(SourceClass::class, priority = 123)
class TargetClass(val property: String) {
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter

            @GeneratedKonverter(priority = 123)
            public fun TargetClass.Companion.fromSourceClass(sourceClass: SourceClass): TargetClass =
                TargetClass(
              property = sourceClass.property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun generateGeneratedKonverterAnnotationForKonverter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    @Konvert(priority = 123)
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter

            public object MapperImpl : Mapper {
              @GeneratedKonverter(priority = 123)
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotGenerateGeneratedKonverterAnnotationIfOptionDisabledInKonvertToOptions() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Konfig

@KonvertTo(TargetClass::class, options=[Konfig(key = "${ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key}", value = "false")])
class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotGenerateGeneratedKonverterAnnotationIfOptionDisabledInKonvertFromOptions() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Konfig

class SourceClass(val property: String)

@KonvertFrom(SourceClass::class, options=[Konfig(key = "${ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key}", value = "false")])
class TargetClass(val property: String) {
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun TargetClass.Companion.fromSourceClass(sourceClass: SourceClass): TargetClass =
                TargetClass(
              property = sourceClass.property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotGenerateGeneratedKonverterAnnotationIfOptionDisabledInKonvertOptions() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konfig

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    @Konvert(options=[Konfig(key = "${ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key}", value = "false")])
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotGenerateGeneratedKonverterAnnotationIfOptionDisabledInKonverterOptions() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konfig

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter(options=[Konfig(key = "${ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key}", value = "false")])
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}


data class SomeTestClass(val s: String)
data class SomeOtherTestClass(val s: Int) {
    companion object
}

interface SomeTestMapper {
    fun toSomeOtherTestClassMETA_INF(source: SomeTestClass): SomeOtherTestClass
    fun toSomeOtherTestClass(source: SomeTestClass): SomeOtherTestClass
    fun toSomeOtherTestClasses(source: List<SomeTestClass>): List<SomeOtherTestClass>
    fun fromSomeOtherTestClass(source: SomeOtherTestClass): SomeTestClass = SomeTestClass(source.s.toString())
}

interface SomeSecondTestMapper {
    fun fromSomeOtherTestClasses(source: List<SomeOtherTestClass>): List<SomeTestClass>
}

/**
 * Is referenced by @GeneratedKonvertModule annotation on [generated.io.mcarle.konvert.TestModule]
 */
@GeneratedKonverter(priority = 10)
fun SomeTestClass.toSomeOtherTestClass() = SomeOtherTestClass(s.toInt())

/**
 * Is referenced by META-INF/konvert/io.mcarle.konvert.api.KonvertTo
 */
@GeneratedKonverter(priority = 2000)
fun SomeTestClass.toSomeOtherTestClassMETA_INF() = SomeOtherTestClass(s.toInt())

/**
 * Is referenced by @GeneratedKonvertModule annotation on [generated.io.mcarle.konvert.TestModule]
 */
@GeneratedKonverter(priority = 11)
fun SomeOtherTestClass.Companion.fromSomeTestClass(source: SomeTestClass) = SomeOtherTestClass(
    source.s.toInt()
)

/**
 * Is referenced by META-INF/konvert/io.mcarle.konvert.api.KonvertFrom
 */
@GeneratedKonverter(priority = 2000)
fun SomeOtherTestClass.Companion.fromSomeTestClassMETA_INF(source: SomeTestClass) = SomeOtherTestClass(
    source.s.toInt()
)

/**
 * Used to test, that a generated konverter as OBJECT can be loaded and used by Konvert
 */
object SomeTestMapperImpl : SomeTestMapper {

    /**
     * Is referenced by META-INF/konvert/io.mcarle.konvert.api.Konvert
     */
    @GeneratedKonverter(priority = 2000)
    override fun toSomeOtherTestClassMETA_INF(source: SomeTestClass): SomeOtherTestClass {
        return SomeOtherTestClass(source.s.toInt())
    }

    /**
     * Is referenced by @GeneratedKonvertModule annotation on [generated.io.mcarle.konvert.TestModule]
     */
    @GeneratedKonverter(priority = 12)
    override fun toSomeOtherTestClass(source: SomeTestClass): SomeOtherTestClass {
        return SomeOtherTestClass(source.s.toInt())
    }

    /**
     * Is referenced by @GeneratedKonvertModule annotation on [generated.io.mcarle.konvert.TestModule]
     */
    @GeneratedKonverter(priority = 123)
    override fun fromSomeOtherTestClass(source: SomeOtherTestClass): SomeTestClass {
        return super.fromSomeOtherTestClass(source)
    }

    /**
     * Is referenced by @GeneratedKonvertModule annotation on [generated.io.mcarle.konvert.TestModule]
     */
    @GeneratedKonverter(priority = 333)
    override fun toSomeOtherTestClasses(source: List<SomeTestClass>): List<SomeOtherTestClass> {
        return source.map { toSomeOtherTestClass(it) }
    }
}

/**
 * Used to test, that a generated konverter as CLASS can be loaded and used by Konvert
 */
class SomeSecondTestMapperImpl : SomeSecondTestMapper {

    /**
     * Is referenced by @GeneratedKonvertModule annotation on [generated.io.mcarle.konvert.TestModule]
     */
    @GeneratedKonverter(priority = 999)
    override fun fromSomeOtherTestClasses(source: List<SomeOtherTestClass>): List<SomeTestClass> {
        return source.map { SomeTestMapperImpl.fromSomeOtherTestClass(it) }
    }
}


