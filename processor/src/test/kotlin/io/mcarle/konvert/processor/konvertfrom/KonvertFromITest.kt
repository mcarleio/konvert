package io.mcarle.konvert.processor.konvertfrom

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.DEFAULT_KONVERT_FROM_PRIORITY
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
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

}
