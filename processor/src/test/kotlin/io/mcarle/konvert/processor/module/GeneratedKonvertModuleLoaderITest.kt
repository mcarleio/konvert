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
class GeneratedKonvertModuleLoaderITest : KonverterITest() {

    @Test
    fun loadGeneratedKonvertTypeConverter() {
        compileWith(enabledConverters = emptyList(), code = emptyArray())
        val alreadyGeneratedKonverterList = TypeConverterRegistry
            .filterIsInstance<KonvertTypeConverter>()
            .filter { it.alreadyGenerated }
        assertEquals(4, alreadyGeneratedKonverterList.size, "missing generated konverter")
        alreadyGeneratedKonverterList[0].let { converter ->
            assertEquals("SomeTestClass", converter.sourceType.toClassName().simpleName)
            assertEquals("SomeOtherTestClass", converter.targetType.toClassName().simpleName)

            assertEquals("toSomeOtherTestClass", converter.mapFunctionName)
            assertEquals("source", converter.paramName)
            assertEquals("SomeTestMapper", converter.konverterInterface.simpleName)
            assertEquals(true, converter.enabledByDefault)
            assertEquals(12, converter.priority)
            assertEquals(KonvertTypeConverter.ClassOrObject.OBJECT, converter.classKind)
        }
        alreadyGeneratedKonverterList[1].let { converter ->
            assertEquals("SomeOtherTestClass", converter.sourceType.toClassName().simpleName)
            assertEquals("SomeTestClass", converter.targetType.toClassName().simpleName)

            assertEquals("fromSomeOtherTestClass", converter.mapFunctionName)
            assertEquals("source", converter.paramName)
            assertEquals("SomeTestMapper", converter.konverterInterface.simpleName)
            assertEquals(true, converter.enabledByDefault)
            assertEquals(123, converter.priority)
            assertEquals(KonvertTypeConverter.ClassOrObject.OBJECT, converter.classKind)
        }
        alreadyGeneratedKonverterList[2].let { converter ->
            // toClassName() would result in exception due to Resolver not initialized
            assertEquals("List<SomeTestClass>", converter.sourceType.toString())
            assertEquals("List<SomeOtherTestClass>", converter.targetType.toString())

            assertEquals("toSomeOtherTestClasses", converter.mapFunctionName)
            assertEquals("source", converter.paramName)
            assertEquals("SomeTestMapper", converter.konverterInterface.simpleName)
            assertEquals(true, converter.enabledByDefault)
            assertEquals(333, converter.priority)
            assertEquals(KonvertTypeConverter.ClassOrObject.OBJECT, converter.classKind)
        }
        alreadyGeneratedKonverterList[3].let { converter ->
            // toClassName() would result in exception due to Resolver not initialized
            assertEquals("List<SomeOtherTestClass>", converter.sourceType.toString())
            assertEquals("List<SomeTestClass>", converter.targetType.toString())

            assertEquals("fromSomeOtherTestClasses", converter.mapFunctionName)
            assertEquals("source", converter.paramName)
            assertEquals("SomeSecondTestMapper", converter.konverterInterface.simpleName)
            assertEquals(true, converter.enabledByDefault)
            assertEquals(999, converter.priority)
            assertEquals(KonvertTypeConverter.ClassOrObject.CLASS, converter.classKind)
        }
    }

    @Test
    fun loadGeneratedKonvertToTypeConverter() {
        compileWith(enabledConverters = emptyList(), code = emptyArray())
        val alreadyGeneratedKonverterList = TypeConverterRegistry
            .filterIsInstance<KonvertToTypeConverter>()
            .filter { it.alreadyGenerated }
        assertEquals(1, alreadyGeneratedKonverterList.size, "missing generated konverter")
        val converter = alreadyGeneratedKonverterList.first()
        assertEquals("toSomeOtherTestClass", converter.mapFunctionName)
        assertEquals("SomeTestClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("SomeOtherTestClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(10, converter.priority)
    }

    @Test
    fun loadGeneratedKonvertFromTypeConverter() {
        compileWith(enabledConverters = emptyList(), code = emptyArray())
        val alreadyGeneratedKonverterList = TypeConverterRegistry
            .filterIsInstance<KonvertFromTypeConverter>()
            .filter { it.alreadyGenerated }
        assertEquals(1, alreadyGeneratedKonverterList.size, "missing generated konverter")
        val converter = alreadyGeneratedKonverterList.first()
        assertEquals("fromSomeTestClass", converter.mapFunctionName)
        assertEquals("SomeTestClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("SomeOtherTestClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals("source", converter.paramName)
        assertEquals(true, converter.enabledByDefault)
        assertEquals(11, converter.priority)
    }

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
