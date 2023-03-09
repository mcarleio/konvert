package io.mcarle.lib.kmapper.processor.converter.kmapper

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.lib.kmapper.converter.SameTypeConverter
import io.mcarle.lib.kmapper.converter.api.DEFAULT_KMAPPER_PRIORITY
import io.mcarle.lib.kmapper.converter.api.TypeConverterRegistry
import io.mcarle.lib.kmapper.processor.converter.ConverterITest
import io.mcarle.lib.kmapper.processor.converter.generatedSourceFor
import io.mcarle.lib.kmapper.processor.kmapper.KMapperConverter
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KMapperConverterITest : ConverterITest() {

    @Test
    fun converter() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.lib.kmapper.api.annotation.KMapper
import io.mcarle.lib.kmapper.api.annotation.KMapping
import io.mcarle.lib.kmapper.api.annotation.KMap

class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
)

@KMapper
interface Mapper {
    @KMapping(mappings = [KMap(source="sourceProperty",target="targetProperty")])
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKMap.kt")
        println(mapperCode)

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KMapperConverter>()
        assertNotNull(converter, "No KMapperConverter registered")
        assertEquals("toTarget", converter.mapFunctionName)
        assertEquals("source", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals("Mapper", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KMAPPER_PRIORITY, converter.priority)
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.lib.kmapper.api.annotation.KMapper
import io.mcarle.lib.kmapper.api.annotation.KMapping
import io.mcarle.lib.kmapper.api.annotation.KMap

class SourceClass(
    val sourceProperty: SourceProperty
)
class TargetClass(
    val targetProperty: TargetProperty
)

@KMapper
interface Mapper {
    @KMapping(mappings = [KMap(source="sourceProperty",target="targetProperty")])
    fun toTarget(source: SourceClass): TargetClass
}

data class SourceProperty(val value: String)
data class TargetProperty(val value: String)

@KMapper
interface OtherMapper {
    @KMapping
    fun toTarget(source: SourceProperty): TargetProperty
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKMap.kt")
        println(mapperCode)

        assertContains(mapperCode, "KMappers.get<OtherMapper>().toTarget(")
    }

}