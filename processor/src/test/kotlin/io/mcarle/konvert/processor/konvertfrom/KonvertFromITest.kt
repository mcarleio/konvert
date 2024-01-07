package io.mcarle.konvert.processor.konvertfrom

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.DEFAULT_KONVERT_FROM_PRIORITY
import io.mcarle.konvert.converter.IterableToIterableConverter
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.GENERATED_FILENAME_SUFFIX_OPTION
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCompilerApi::class)
class KonvertFromITest : KonverterITest() {

    @Test
    fun annotationOnCompanionObject() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
) {
    @KonvertFrom(SourceClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")
        println(extensionFunctionCode)

        val converter = TypeConverterRegistry.filterIsInstance<KonvertFromTypeConverter>().firstOrNull {
            !it.alreadyGenerated
        }
        assertNotNull(converter, "No KonvertFromTypeConverter registered")
        assertEquals("fromSourceClass", converter.mapFunctionName)
        assertEquals("sourceClass", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KONVERT_FROM_PRIORITY, converter.priority)
    }

    @Test
    fun annotationOnClass() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: String
)
@KonvertFrom(SourceClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
class TargetClass(
    val targetProperty: String
) {
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")
        println(extensionFunctionCode)

        val converter = TypeConverterRegistry.filterIsInstance<KonvertFromTypeConverter>().firstOrNull {
            !it.alreadyGenerated
        }
        assertNotNull(converter, "No KonvertFromTypeConverter registered")
        assertEquals("fromSourceClass", converter.mapFunctionName)
        assertEquals("sourceClass", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KONVERT_FROM_PRIORITY, converter.priority)
    }

    @Test
    fun failOnAnnotationOnClassWithGenerics() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: String
)
@KonvertFrom(SourceClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
class TargetClass<T>(
    val targetProperty: T
) {
    companion object
}
                """.trimIndent()
            )
        )

        assertContains(result.messages, "@KonvertFrom not allowed on classes with generics: TargetClass")
    }

    @Test
    fun failOnAnnotationOnClassWithoutCompanion() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: String
)
@KonvertFrom(SourceClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
class TargetClass(
    val targetProperty: String
)
                """.trimIndent()
            )
        )

        assertContains(result.messages, "Missing companion in TargetClass")
    }

    @Test
    fun failOnAnnotationOnCompanionOfAnnotationClass() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: String
)
annotation class TargetClass(
    val targetProperty: String
) {
    @KonvertFrom(SourceClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
    companion object
}
                """.trimIndent()
            )
        )

        assertContains(result.messages, "Parent of TargetClass.Companion is not a class: TargetClass")
    }

    @Test
    fun failOnAnnotationOnCompanionOfClassWithGenerics() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: String
)
class TargetClass<T>(
    val targetProperty: T
) {
    @KonvertFrom(SourceClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
    companion object
}
                """.trimIndent()
            )
        )

        assertContains(result.messages, "@KonvertFrom not allowed on classes with generics: TargetClass")
    }

    @Test
    fun failOnAnnotatingAnObject() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: String
)
@KonvertFrom(SourceClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
object TargetClass {
    val targetProperty: String
}
                """.trimIndent()
            )
        )

        assertContains(
            result.messages,
            "@KonvertFrom only allowed on companion objects or class declarations with a companion, but TargetClass is neither"
        )
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: SourceProperty
)
class TargetClass(
    val targetProperty: TargetProperty
) {
    @KonvertFrom(SourceClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
    companion object
}

data class SourceProperty(val value: String)
@KonvertFrom(SourceProperty::class)
data class TargetProperty(val value: String) {
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "TargetProperty.fromSourceProperty(sourceProperty = sourceClass.sourceProperty)")
    }


    @Test
    fun handleDifferentPackages() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "a/SourceClass.kt",
                    contents =
                    """
package a

class SourceClass(val property: String)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/TargetClass.kt",
                    contents =
                    """
package b

import io.mcarle.konvert.api.KonvertFrom
import a.SourceClass

@KonvertFrom(SourceClass::class)
class TargetClass {
    companion object
    var property: String = ""
}
                    """.trimIndent()
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            package b

            import a.SourceClass

            public fun TargetClass.Companion.fromSourceClass(sourceClass: SourceClass): TargetClass =
                TargetClass().also { targetClass ->
              targetClass.property = sourceClass.property
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun handleSameClassNameInDifferentPackages() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "a/SomeClass.kt",
                    contents =
                    """
package a

class SomeClass(val property: String)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/SomeClass.kt",
                    contents =
                    """
package b
import io.mcarle.konvert.api.KonvertFrom

@KonvertFrom(a.SomeClass::class)
class SomeClass {
    companion object
    var property: String = ""
}
                    """.trimIndent()
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SomeClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            package b

            public fun SomeClass.Companion.fromSomeClass(someClass: a.SomeClass): SomeClass =
                SomeClass().also { someClass0 ->
              someClass0.property = someClass.property
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun handleSameClassNameInDifferentPackagesWithImportAlias() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "a/SomeClass.kt",
                    contents =
                    """
package a


class SomeClass(val property: String)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/SomeClass.kt",
                    contents =
                    """
package b

import io.mcarle.konvert.api.KonvertFrom
import a.SomeClass as A

@KonvertFrom(A::class)
class SomeClass {
    companion object
    var property: String = ""
}
                    """.trimIndent()
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SomeClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            package b

            public fun SomeClass.Companion.fromSomeClass(someClass: a.SomeClass): SomeClass =
                SomeClass().also { someClass0 ->
              someClass0.property = someClass.property
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "GlobalSuffix,LocalSuffix,LocalSuffix",
            "GlobalSuffix,,GlobalSuffix",
            ",LocalSuffix,LocalSuffix",
            ",,Konverter",
        ]
    )
    fun configurationTest(globalSuffix: String?, localSuffix: String?, expectedSuffix: String) {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = if (globalSuffix != null) {
                mapOf(GENERATED_FILENAME_SUFFIX_OPTION.key to globalSuffix)
            } else {
                mapOf()
            },
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents = // @formatter:off
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Konfig

${if (localSuffix != null) {
    """@KonvertFrom(SourceClass::class, options=[Konfig(key="${GENERATED_FILENAME_SUFFIX_OPTION.key}", value="$localSuffix")])"""
} else {
    """@KonvertFrom(SourceClass::class)"""
}}
data class TargetClass(val property: String) { companion object }
data class SourceClass(val property: String)
                """.trimIndent() // @formatter:on
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClass${expectedSuffix}.kt")
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
    fun recursiveTreeMap() {
        val (compilation) = compileWith(
            enabledConverters = listOf(IterableToIterableConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Mapping

class SourceClass(val children: List<SourceClass>)
class TargetClass(val children: List<TargetClass>) {
    @KonvertFrom(SourceClass::class)
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
              children = sourceClass.children.map { TargetClass.fromSourceClass(sourceClass = it) }
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}
