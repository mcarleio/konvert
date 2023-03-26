package io.mcarle.kmap.processor.kmapper

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.kmap.api.annotation.KMap
import io.mcarle.kmap.converter.SameTypeConverter
import io.mcarle.kmap.converter.api.DEFAULT_KMAPPER_NO_ANNOTATION_PRIORITY
import io.mcarle.kmap.converter.api.DEFAULT_KMAPPER_PRIORITY
import io.mcarle.kmap.converter.api.TypeConverterRegistry
import io.mcarle.kmap.processor.ConverterITest
import io.mcarle.kmap.processor.generatedSourceFor
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import io.mcarle.kmap.api.annotation.KMapper
import io.mcarle.kmap.api.annotation.KMapping
import io.mcarle.kmap.api.annotation.KMap

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
        assertEquals(listOf(KMap(source = "sourceProperty", target = "targetProperty")), converter.annotation?.mappings)
        assertEquals(DEFAULT_KMAPPER_PRIORITY, converter.priority)
    }

    @Test
    fun defaultMappingsOnMissingKMappingAnnotation() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.kmap.api.annotation.KMapper

class SourceClass(
    val property: String
)
class TargetClass(
    val property: String
)

@KMapper
interface Mapper {
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
        assertEquals(listOf(), converter.annotation?.mappings)
        assertEquals(DEFAULT_KMAPPER_PRIORITY, converter.priority)
    }

    @Test
    fun registerConverterForImplementedFunctions() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.kmap.api.annotation.KMapper

class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
)

@KMapper
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass {
        return TargetClass(source.sourceProperty)
    }
}
                """.trimIndent()
            )
        )
        assertThrows<IllegalArgumentException> { compilation.generatedSourceFor("MapperKMap.kt") }

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KMapperConverter>()
        assertNotNull(converter, "No KMapperConverter registered")
        assertEquals("toTarget", converter.mapFunctionName)
        assertEquals("source", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals("Mapper", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(null, converter.annotation)
        assertEquals(DEFAULT_KMAPPER_NO_ANNOTATION_PRIORITY, converter.priority)
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.kmap.api.annotation.KMapper
import io.mcarle.kmap.api.annotation.KMapping
import io.mcarle.kmap.api.annotation.KMap

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