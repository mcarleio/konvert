package io.mcarle.lib.kmapper.processor.converter.annotated

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.lib.kmapper.converter.SameTypeConverter
import io.mcarle.lib.kmapper.converter.api.DEFAULT_KMAPTO_PRIORITY
import io.mcarle.lib.kmapper.converter.api.TypeConverterRegistry
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KMapToConverterITest : ConverterITest() {

    @Test
    fun converter() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.lib.kmapper.api.annotation.KMapTo
import io.mcarle.lib.kmapper.api.annotation.KMap

@KMapTo(TargetClass::class, mappings=[KMap(source="sourceProperty", target="targetProperty")])
class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKMapExtensions.kt")
        println(extensionFunctionCode)

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KMapToConverter>()
        assertNotNull(converter, "No KMapToConverter registered")
        assertEquals("mapToTargetClass", converter.mapFunctionName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        // TODO: remove mapKSClassDeclaration?
        assertEquals("SourceClass", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KMAPTO_PRIORITY, converter.priority)
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.lib.kmapper.api.annotation.KMapTo
import io.mcarle.lib.kmapper.api.annotation.KMap

@KMapTo(TargetClass::class, mappings=[KMap(source="sourceProperty", target="targetProperty")])
class SourceClass(
    val sourceProperty: SourceProperty
)
class TargetClass(
    val targetProperty: TargetProperty
)

@KMapTo(TargetProperty::class)
data class SourceProperty(val value: String)
data class TargetProperty(val value: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKMapExtensions.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "sourceProperty.mapToTargetProperty()")
    }

}