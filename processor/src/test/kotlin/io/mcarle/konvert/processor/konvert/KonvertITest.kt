package io.mcarle.konvert.processor.konvert

import com.squareup.kotlinpoet.ksp.toClassName
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.DEFAULT_KONVERTER_PRIORITY
import io.mcarle.konvert.api.DEFAULT_KONVERT_PRIORITY
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.config.GENERATED_FILENAME_SUFFIX
import io.mcarle.konvert.api.config.KONVERTER_GENERATE_CLASS
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KonvertITest : KonverterITest() {

    @Test
    fun converter() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(source="sourceProperty",target="targetProperty")])
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KonvertTypeConverter>()
        assertNotNull(converter, "No KonverterTypeConverter registered")
        assertEquals("toTarget", converter.mapFunctionName)
        assertEquals("source", converter.paramName)
        assertEquals("SourceClass", converter.sourceType.toClassName().simpleName)
        assertEquals("TargetClass", converter.targetType.toClassName().simpleName)
        assertEquals("Mapper", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KONVERT_PRIORITY, converter.priority)
    }

    @Test
    fun defaultMappingsOnMissingKonvertAnnotation() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val property: String
)
class TargetClass(
    val property: String
)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KonvertTypeConverter>()
        assertNotNull(converter, "No KonverterTypeConverter registered")
        assertEquals("toTarget", converter.mapFunctionName)
        assertEquals("source", converter.paramName)
        assertEquals("SourceClass", converter.sourceType.toClassName().simpleName)
        assertEquals("TargetClass", converter.targetType.toClassName().simpleName)
        assertEquals("Mapper", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KONVERT_PRIORITY, converter.priority)
    }

    @Test
    fun registerConverterForImplementedFunctions() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass {
        return TargetClass(source.sourceProperty)
    }
}
                """.trimIndent()
            )
        )
        assertThrows<IllegalArgumentException> { compilation.generatedSourceFor("MapperKonverter.kt") }

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KonvertTypeConverter>()
        assertNotNull(converter, "No KonverterTypeConverter registered")
        assertEquals("toTarget", converter.mapFunctionName)
        assertEquals("source", converter.paramName)
        assertEquals("SourceClass", converter.sourceType.toClassName().simpleName)
        assertEquals("TargetClass", converter.targetType.toClassName().simpleName)
        assertEquals("Mapper", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(DEFAULT_KONVERTER_PRIORITY, converter.priority)
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: SourceProperty
)
class TargetClass(
    val targetProperty: TargetProperty
)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(source="sourceProperty",target="targetProperty")])
    fun toTarget(source: SourceClass): TargetClass
}

data class SourceProperty(val value: String)
data class TargetProperty(val value: String)

@Konverter
interface OtherMapper {
    fun toTarget(source: SourceProperty): TargetProperty
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "Konverter.get<OtherMapper>().toTarget(")
    }

    @Test
    fun handleNullableOfSourceParamWhenUsingOtherKonverterWithSourceNullable() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val property: SourceProperty?
)
class TargetClass(
    val property: TargetProperty?
)
data class SourceProperty(
    val prop: String
)
data class TargetProperty(
    val prop: String
)

@Konverter
interface Mapper {
    @Konvert
    fun toTarget(sourceClass: SourceClass?): TargetClass?
    fun toTarget(source: SourceProperty?): TargetProperty? = source?.prop?.let { TargetProperty(prop = it) }
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "source = sourceClass?.property")
    }

    @Test
    fun handleNullableOfSourceParamWhenUsingOtherKonverter() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val property: SourceProperty?
)
class TargetClass(
    val property: TargetProperty?
)
data class SourceProperty(
    val prop: String
)
data class TargetProperty(
    val prop: String
)

@Konverter
interface Mapper {
    @Konvert
    fun toTarget(sourceClass: SourceClass?): TargetClass?
    @Konvert
    fun toTarget(source: SourceProperty): TargetProperty
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "sourceClass?.property?.let {")
    }

    @Test
    fun useSelfImplementedKonverter() {
        val (compilation) = super.compileWith(
            listOf(),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class SourceClass(val property: String)
class SourceOptionalClass(val property: String?)
class TargetClass(val property: Int)
class TargetOptionalClass(val property: Int?)

@Konverter
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass): TargetClass
    @Konvert
    fun toTargetOptional(source: SourceClass): TargetOptionalClass
    @Konvert
    fun toTarget(source: SourceOptionalClass): TargetClass
    @Konvert
    fun toTargetOptional(source: SourceOptionalClass): TargetOptionalClass
}

@Konverter
interface OtherMapper {
    fun toInt(source: String): Int = source.toInt()
    fun optionalToInt(source: String?): Int = source?.toInt() ?: 0
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "Konverter.get<OtherMapper>().toInt(")
        assertContains(mapperCode, "source.property?.let {")
    }

    @Test
    fun useSelfImplementedKonverterWithGenerics() {
        val (compilation) = super.compileWith(
            listOf(),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class SourceClass(val property: List<String>)
class SourceOptionalClass(val property: List<String?>)
class TargetClass(val property: List<Int>)
class TargetOptionalClass(val property: List<Int?>)

@Konverter
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass): TargetClass
    @Konvert
    fun toTargetOptional(source: SourceClass): TargetOptionalClass
    @Konvert
    fun toTarget(source: SourceOptionalClass): TargetClass
    @Konvert
    fun toTargetOptional(source: SourceOptionalClass): TargetOptionalClass
}

@Konverter
interface OtherMapper {
    fun toListOfInts(source: List<String>): List<Int> = source.map { it.toInt() }
    fun optionalToListOfInts(source: List<String?>): List<Int> = source.map { it?.toInt() ?: 0 }
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "Konverter.get<OtherMapper>().toListOfInts(")
        assertContains(mapperCode, "Konverter.get<OtherMapper>().optionalToListOfInts(")
    }

    @Test
    fun configGenerateClassInsteadOfObject() {
        val (compilation, result) = super.compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            otherConverters = emptyList(),
            expectSuccess = true,
            options = mapOf(KONVERTER_GENERATE_CLASS to "false"),
            SourceFile.kotlin(
                name = "SourceClass.kt",
                contents =
                """
class SourceClass(val property: String)
                """.trimIndent()
            ),
            SourceFile.kotlin(
                name = "TargetClass.kt",
                contents =
                """
class TargetClass {
    var property: String = ""
}
                """.trimIndent()
            ),
            SourceFile.kotlin(
                name = "SomeConverter.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konfig

@Konverter(options=[Konfig(key = "$KONVERTER_GENERATE_CLASS", value = "true")])
interface SomeConverter {
    @Konvert
    fun toTargetClass(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public class SomeConverterImpl : SomeConverter {
              public override fun toTargetClass(source: SourceClass): TargetClass =
                  TargetClass().also { targetClass ->
                targetClass.property = source.property
              }
            }
            """.trimIndent(),
            extensionFunctionCode
        )
        Konverter.addClassLoader(result.classLoader)
        val instance = Konverter.get(result.classLoader.loadClass("SomeConverter").kotlin)
        assertNotNull(instance)
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

class TargetClass {
    var property: String = ""
}
                """.trimIndent()
            ),
            SourceFile.kotlin(
                name = "SomeConverter.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import a.SourceClass
import b.TargetClass

@Konverter
interface SomeConverter {
    @Konvert
    fun toTargetClass(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import a.SourceClass
            import b.TargetClass

            public object SomeConverterImpl : SomeConverter {
              public override fun toTargetClass(source: SourceClass): TargetClass =
                  TargetClass().also { targetClass ->
                targetClass.property = source.property
              }
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }


    @Test
    fun handleSameClassNameInDifferentPackagesWithFQN() {
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

class SomeClass {
    var property: String = ""
}
                """.trimIndent()
            ),
            SourceFile.kotlin(
                name = "SomeConverter.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import a.SomeClass

@Konverter
interface SomeConverter {
    @Konvert
    fun toSomeClass(source: SomeClass): b.SomeClass
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import a.SomeClass

            public object SomeConverterImpl : SomeConverter {
              public override fun toSomeClass(source: SomeClass): b.SomeClass =
                  b.SomeClass().also { someClass ->
                someClass.property = source.property
              }
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun handleSameClassNameInDifferentPackagesWithFQNAndNullable() {
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

class SomeClass {
    var property: String = ""
}
                """.trimIndent()
            ),
            SourceFile.kotlin(
                name = "SomeConverter.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import a.SomeClass

@Konverter
interface SomeConverter {
    @Konvert
    fun toSomeClass(source: SomeClass): b.SomeClass?
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import a.SomeClass

            public object SomeConverterImpl : SomeConverter {
              public override fun toSomeClass(source: SomeClass): b.SomeClass? =
                  b.SomeClass().also { someClass ->
                someClass.property = source.property
              }
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

class SomeClass {
    var property: String = ""
}
                """.trimIndent()
            ),
            SourceFile.kotlin(
                name = "SomeConverter.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import a.SomeClass
import b.SomeClass as B

@Konverter
interface SomeConverter {
    @Konvert
    fun toB(source: SomeClass): B
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import a.SomeClass
            import b.SomeClass as B

            public object SomeConverterImpl : SomeConverter {
              public override fun toB(source: SomeClass): B = B().also { someClass ->
                someClass.property = source.property
              }
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun handleSameClassNameInDifferentPackagesWithImportAliasWithNullable() {
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

class SomeClass {
    var property: String = ""
}
                """.trimIndent()
            ),
            SourceFile.kotlin(
                name = "SomeConverter.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import a.SomeClass
import b.SomeClass as B

@Konverter
interface SomeConverter {
    @Konvert
    fun toB(source: SomeClass): B?
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import a.SomeClass
            import b.SomeClass as B

            public object SomeConverterImpl : SomeConverter {
              public override fun toB(source: SomeClass): B? = B().also { someClass ->
                someClass.property = source.property
              }
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
                mapOf(GENERATED_FILENAME_SUFFIX to globalSuffix)
            } else {
                mapOf()
            },
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents = // @formatter:off
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konfig

${if (localSuffix != null) {
    """@Konverter(options=[Konfig(key="$GENERATED_FILENAME_SUFFIX", value="$localSuffix")])"""
} else {
    """@Konverter"""
}}
interface MyMapper {
    fun toTarget(source: SourceClass): TargetClass
}
data class SourceClass(val property: String)
data class TargetClass(val property: String)
                """.trimIndent() // @formatter:on
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MyMapper${expectedSuffix}.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public object MyMapperImpl : MyMapper {
              public override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}
