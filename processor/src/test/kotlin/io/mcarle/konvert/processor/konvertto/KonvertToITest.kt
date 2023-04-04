package io.mcarle.konvert.processor.konvertto

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.DEFAULT_KONVERT_TO_PRIORITY
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KonvertToITest : KonverterITest() {

    @Test
    fun converter() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
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

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KonvertToTypeConverter>()
        assertNotNull(converter, "No KonvertToTypeConverter registered")
        assertEquals("toTargetClass", converter.mapFunctionName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KONVERT_TO_PRIORITY, converter.priority)
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
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
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
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
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
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

        assertContains(extensionFunctionCode, "get<KonvertInterface>().toTargetProperty(sourceProperty = ")
    }

}
