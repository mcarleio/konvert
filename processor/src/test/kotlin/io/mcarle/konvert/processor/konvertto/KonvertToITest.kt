package io.mcarle.konvert.processor.konvertto

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.DEFAULT_KONVERT_TO_PRIORITY
import io.mcarle.konvert.converter.IterableToIterableConverter
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.GENERATED_FILENAME_SUFFIX_OPTION
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KonvertToITest : KonverterITest() {

    @Test
    fun converter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        val converter = TypeConverterRegistry.filterIsInstance<KonvertToTypeConverter>().firstOrNull {
            !it.alreadyGenerated
        }
        assertNotNull(converter, "No KonvertToTypeConverter registered")
        assertEquals("toTargetClass", converter.mapFunctionName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KONVERT_TO_PRIORITY, converter.priority)
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
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
class SourceClass<T>(
    val sourceProperty: T
)
class TargetClass(
    val targetProperty: String
)
                """.trimIndent()
            )
        )

        assertContains(result.messages, "@KonvertTo not allowed on types with generics: SourceClass")
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
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
object SourceClass {
    val sourceProperty: String
}
class TargetClass(
    val targetProperty: String
)
                """.trimIndent()
            )
        )

        assertContains(result.messages, "@KonvertTo can only target classes, but SourceClass is not a class")
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
class SourceClass(
    val sourceProperty: SourceProperty
)
class TargetClass(
    val targetProperty: TargetProperty
)

@KonvertTo(TargetProperty::class)
data class SourceProperty(val value: String)
data class TargetProperty(val value: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "sourceProperty.toTargetProperty()")
    }

    @Test
    fun useOtherMapperOnNull() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
class SourceClass(
    val sourceProperty: SourceProperty
)
class TargetClass(
    val targetProperty: TargetProperty?
)

@KonvertTo(TargetProperty::class)
data class SourceProperty(val value: String)
data class TargetProperty(val value: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "sourceProperty.toTargetProperty()")
    }

    @Test
    fun useOtherMapperWithList() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[Mapping(source="sourceProperty", target="targetProperty")])
class SourceClass(
    val sourceProperty: List<SourceProperty<String>>
)
class TargetClass(
    val targetProperty: List<TargetProperty<String>>
)

@Konverter
interface KonvertInterface {
    fun toTargetProperty(sourceProperty: List<SourceProperty<String>>): List<TargetProperty<String>> = listOf()
}

class SourceProperty<E>(val value: E)
class TargetProperty<E>(val value: E)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty = KonvertInterfaceImpl.toTargetProperty(sourceProperty = sourceProperty)
            )
        """.trimIndent(), extensionFunctionCode
        )
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

import io.mcarle.konvert.api.KonvertTo
import b.TargetClass

@KonvertTo(TargetClass::class)
class SourceClass(val property: String)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/TargetClass.kt",
                    contents =
                    """
package b

class TargetClass {
    var property: String = ""
}
                    """.trimIndent()
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            package a

            import b.TargetClass

            public fun SourceClass.toTargetClass(): TargetClass = TargetClass().also { targetClass ->
              targetClass.property = property
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

import io.mcarle.konvert.api.KonvertTo

@KonvertTo(b.SomeClass::class)
class SomeClass(val property: String)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/SomeClass.kt",
                    contents =
                    """
package b

class SomeClass {
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
            package a

            public fun SomeClass.toSomeClass(): b.SomeClass = b.SomeClass().also { someClass ->
              someClass.property = property
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

import io.mcarle.konvert.api.KonvertTo
import b.SomeClass as B

@KonvertTo(B::class)
class SomeClass(val property: String)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/SomeClass.kt",
                    contents =
                    """
package b

class SomeClass {
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
            package a

            public fun SomeClass.toSomeClass(): b.SomeClass = b.SomeClass().also { someClass ->
              someClass.property = property
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
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Konfig

${if (localSuffix != null) {
    """@KonvertTo(TargetClass::class, options=[Konfig(key="${GENERATED_FILENAME_SUFFIX_OPTION.key}", value="$localSuffix")])"""
} else {
    """@KonvertTo(TargetClass::class)"""
}}
data class SourceClass(val property: String)
data class TargetClass(val property: String)
                """.trimIndent() // @formatter:on
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClass${expectedSuffix}.kt")
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
    fun recursiveTreeMap() {
        val (compilation) = compileWith(
            enabledConverters = listOf(IterableToIterableConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(val children: List<SourceClass>)
class TargetClass(val children: List<TargetClass>)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              children = children.map { it.toTargetClass() }
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}
