package io.mcarle.konvert.processor.module

import com.squareup.kotlinpoet.ksp.toClassName
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.PARSE_DEPRECATED_META_INF_FILES_OPTION
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.konvert.KonvertTypeConverter
import io.mcarle.konvert.processor.konvertfrom.KonvertFromTypeConverter
import io.mcarle.konvert.processor.konvertto.KonvertToTypeConverter
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
class GeneratedKonverterLoaderFromMetaInfITest : KonverterITest() {

    @Test
    fun loadGeneratedKonvertTypeConverterFromMETA_INF() {
        compileWith(
            enabledConverters = emptyList(),
            code = emptyArray(),
            options = mapOf(
                PARSE_DEPRECATED_META_INF_FILES_OPTION.key to "true"
            )
        )
        val alreadyGeneratedKonverterList = TypeConverterRegistry
            .filterIsInstance<KonvertTypeConverter>()
            .filter { it.alreadyGenerated }
        assertEquals(5, alreadyGeneratedKonverterList.size, "missing generated konverter")
        val converter = alreadyGeneratedKonverterList.last()

        assertEquals("SomeTestClass", converter.sourceType.toClassName().simpleName)
        assertEquals("SomeOtherTestClass", converter.targetType.toClassName().simpleName)

        assertEquals("toSomeOtherTestClassMETA_INF", converter.mapFunctionName)
        assertEquals("source", converter.paramName)
        assertEquals("SomeTestMapper", converter.konverterInterface.simpleName)
        assertEquals(true, converter.enabledByDefault)
        assertEquals(2000, converter.priority)
        assertEquals(KonvertTypeConverter.ClassOrObject.OBJECT, converter.classKind)
    }

    @Test
    fun loadGeneratedKonvertToTypeConverterFromMETA_INF() {
        compileWith(
            enabledConverters = emptyList(),
            code = emptyArray(),
            options = mapOf(
                PARSE_DEPRECATED_META_INF_FILES_OPTION.key to "true"
            )
        )
        val alreadyGeneratedKonverterList = TypeConverterRegistry
            .filterIsInstance<KonvertToTypeConverter>()
            .filter { it.alreadyGenerated }
        assertEquals(2, alreadyGeneratedKonverterList.size, "missing generated konverter")
        val converter = alreadyGeneratedKonverterList.last()
        assertEquals("toSomeOtherTestClassMETA_INF", converter.mapFunctionName)
        assertEquals("SomeTestClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("SomeOtherTestClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(2000, converter.priority)
    }

    @Test
    fun loadGeneratedKonvertFromTypeConverterFromMETA_INF() {
        compileWith(
            enabledConverters = emptyList(),
            code = emptyArray(),
            options = mapOf(
                PARSE_DEPRECATED_META_INF_FILES_OPTION.key to "true"
            )
        )
        val alreadyGeneratedKonverterList = TypeConverterRegistry
            .filterIsInstance<KonvertFromTypeConverter>()
            .filter { it.alreadyGenerated }
        assertEquals(2, alreadyGeneratedKonverterList.size, "missing generated konverter")
        val converter = alreadyGeneratedKonverterList.last()
        assertEquals("fromSomeTestClassMETA_INF", converter.mapFunctionName)
        assertEquals("SomeTestClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("SomeOtherTestClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals("source", converter.paramName)
        assertEquals(true, converter.enabledByDefault)
        assertEquals(2000, converter.priority)
    }

}
