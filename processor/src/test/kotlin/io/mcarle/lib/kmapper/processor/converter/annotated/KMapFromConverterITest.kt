package io.mcarle.lib.kmapper.processor.converter.annotated

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.lib.kmapper.converter.SameTypeConverter
import io.mcarle.lib.kmapper.converter.api.DEFAULT_KMAPFROM_PRIORITY
import io.mcarle.lib.kmapper.converter.api.TypeConverterRegistry
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KMapFromConverterITest : ConverterITest() {

    @Test
    fun annotationOnCompanionObject() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.lib.kmapper.api.annotation.KMapFrom
import io.mcarle.lib.kmapper.api.annotation.KMap

class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
) {
    @KMapFrom(SourceClass::class, mappings=[KMap(source="sourceProperty", target="targetProperty")])
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKMapFromExtensions.kt")
        println(extensionFunctionCode)

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KMapFromConverter>()
        assertNotNull(converter, "No KMapFromConverter registered")
        assertEquals("fromSourceClass", converter.mapFunctionName)
        assertEquals("sourceClass", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KMAPFROM_PRIORITY, converter.priority)
    }

    @Test
    fun annotationOnClass() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.lib.kmapper.api.annotation.KMapFrom
import io.mcarle.lib.kmapper.api.annotation.KMap

class SourceClass(
    val sourceProperty: String
)
@KMapFrom(SourceClass::class, mappings=[KMap(source="sourceProperty", target="targetProperty")])
class TargetClass(
    val targetProperty: String
) {
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKMapFromExtensions.kt")
        println(extensionFunctionCode)

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KMapFromConverter>()
        assertNotNull(converter, "No KMapFromConverter registered")
        assertEquals("fromSourceClass", converter.mapFunctionName)
        assertEquals("sourceClass", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KMAPFROM_PRIORITY, converter.priority)
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.lib.kmapper.api.annotation.KMapFrom
import io.mcarle.lib.kmapper.api.annotation.KMap

class SourceClass(
    val sourceProperty: SourceProperty
)
class TargetClass(
    val targetProperty: TargetProperty
) {
    @KMapFrom(SourceClass::class, mappings=[KMap(source="sourceProperty", target="targetProperty")])
    companion object
}

data class SourceProperty(val value: String)
@KMapFrom(SourceProperty::class)
data class TargetProperty(val value: String) {
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKMapFromExtensions.kt")
        println(extensionFunctionCode)

        assertContains(extensionFunctionCode, "TargetProperty.fromSourceProperty(sourceProperty = sourceClass.sourceProperty)")
    }

}