package io.mcarle.konvert.processor.konvertfrom

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.DEFAULT_KONVERT_FROM_PRIORITY
import io.mcarle.konvert.converter.IterableToIterableConverter
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.GENERATED_FILENAME_SUFFIX_OPTION
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KonvertFromITest : KonverterITest() {

    @Test
    fun annotationOnCompanionObject() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
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

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KonvertFromTypeConverter>()
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
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
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

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KonvertFromTypeConverter>()
        assertNotNull(converter, "No KonvertFromTypeConverter registered")
        assertEquals("fromSourceClass", converter.mapFunctionName)
        assertEquals("sourceClass", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KONVERT_FROM_PRIORITY, converter.priority)
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
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
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
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
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
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
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
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
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            if (globalSuffix != null) {
                mapOf(GENERATED_FILENAME_SUFFIX_OPTION.key to globalSuffix)
            } else {
                mapOf()
            },
            SourceFile.kotlin(
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
        val (compilation) = super.compileWith(
            listOf(IterableToIterableConverter()),
            SourceFile.kotlin(
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
