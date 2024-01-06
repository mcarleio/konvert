package io.mcarle.konvert.processor.konvert

import com.squareup.kotlinpoet.ksp.toClassName
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.DEFAULT_KONVERTER_PRIORITY
import io.mcarle.konvert.api.DEFAULT_KONVERT_PRIORITY
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.converter.IntToStringConverter
import io.mcarle.konvert.converter.IterableToIterableConverter
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.GENERATED_FILENAME_SUFFIX_OPTION
import io.mcarle.konvert.converter.api.config.KONVERTER_GENERATE_CLASS_OPTION
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull


class KonvertITest : KonverterITest() {

    @Test
    fun converter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
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

        val converter = TypeConverterRegistry.filterIsInstance<KonvertTypeConverter>().firstOrNull {
            !it.alreadyGenerated
        }
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
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
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

        val converter = TypeConverterRegistry.filterIsInstance<KonvertTypeConverter>().firstOrNull {
            !it.alreadyGenerated
        }
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
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
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
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "super.toTarget(source)")

        val converter = TypeConverterRegistry.filterIsInstance<KonvertTypeConverter>().firstOrNull {
            !it.alreadyGenerated
        }
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
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
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
    fun handleNullableOfSourceAndTargetParam() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "TestCode.kt",
                    contents =
                    """
class SourceClass(val property: String)
class TargetClass(val property: String)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "Mapper.kt",
                    contents =
                    """
import io.mcarle.konvert.api.Konverter

@Konverter
interface Mapper {
fun toTargetClass(source: SourceClass?): TargetClass?
}
                    """.trimIndent()
                )
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
        public object MapperImpl : Mapper {
          override fun toTargetClass(source: SourceClass?): TargetClass? = source?.let {
            TargetClass(
              property = source.property
            )
          }
        }
        """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun handleNullableOfSourceParamWhenUsingOtherKonverterWithSourceNullable() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
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

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(sourceClass: SourceClass?): TargetClass? = sourceClass?.let {
                TargetClass(
                  property = this.toTarget(source = sourceClass.property)
                )
              }

              override fun toTarget(source: SourceProperty?): TargetProperty? = super.toTarget(source)
            }
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun handleNullableOfSourceParamWhenUsingOtherKonverter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
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

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(sourceClass: SourceClass?): TargetClass? = sourceClass?.let {
                TargetClass(
                  property = sourceClass.property?.let { this.toTarget(source = it) }
                )
              }

              override fun toTarget(source: SourceProperty): TargetProperty = TargetProperty(
                prop = source.prop
              )
            }
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun useSelfImplementedKonverter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(),
            code = SourceFile.kotlin(
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
        val (compilation) = compileWith(
            enabledConverters = listOf(),
            code = SourceFile.kotlin(
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
        val (compilation, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(KONVERTER_GENERATE_CLASS_OPTION.key to "false"),
            code = arrayOf(
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

@Konverter(options=[Konfig(key = "${KONVERTER_GENERATE_CLASS_OPTION.key}", value = "true")])
interface SomeConverter {
    @Konvert
    fun toTargetClass(source: SourceClass): TargetClass
}
                    """.trimIndent()
                )
            )
        )
        val mapperCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public class SomeConverterImpl : SomeConverter {
              override fun toTargetClass(source: SourceClass): TargetClass = TargetClass().also { targetClass ->
                targetClass.property = source.property
              }
            }
            """.trimIndent(),
            mapperCode
        )
        Konverter.addClassLoader(result.classLoader)
        val instance = Konverter.get(result.classLoader.loadClass("SomeConverter").kotlin)
        assertNotNull(instance)
    }

    @Test
    fun handleDifferentPackages() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
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
        )
        val mapperCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import a.SourceClass
            import b.TargetClass

            public object SomeConverterImpl : SomeConverter {
              override fun toTargetClass(source: SourceClass): TargetClass = TargetClass().also { targetClass ->
                targetClass.property = source.property
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }


    @Test
    fun handleSameClassNameInDifferentPackagesWithFQN() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
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
        )
        val mapperCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import a.SomeClass as ASomeClass
            import b.SomeClass as BSomeClass

            public object SomeConverterImpl : SomeConverter {
              override fun toSomeClass(source: ASomeClass): BSomeClass = b.SomeClass().also { someClass ->
                someClass.property = source.property
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun handleSameClassNameInDifferentPackagesWithFQNAndNullable() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
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
        )
        val mapperCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import a.SomeClass as ASomeClass
            import b.SomeClass as BSomeClass

            public object SomeConverterImpl : SomeConverter {
              override fun toSomeClass(source: ASomeClass): BSomeClass? = b.SomeClass().also { someClass ->
                someClass.property = source.property
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun handleSameClassNameInDifferentPackagesWithImportAlias() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
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
        )
        val mapperCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import a.SomeClass
            import b.SomeClass as B

            public object SomeConverterImpl : SomeConverter {
              override fun toB(source: SomeClass): B = B().also { someClass ->
                someClass.property = source.property
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }


    @Test
    fun handleSameClassNameInDifferentPackagesWithImportAliasOnSelfImplementedMappingFunctions() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
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
    fun toB(source: SomeClass): B = B().also { someClass ->
        someClass.property = source.property
    }
}
                    """.trimIndent()
                )
            )
        )
        val mapperCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import a.SomeClass
            import b.SomeClass as B

            public object SomeConverterImpl : SomeConverter {
              override fun toB(source: SomeClass): B = super.toB(source)
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun handleSameClassNameInDifferentPackagesWithImportAliasWithNullable() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
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
        )
        val mapperCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import a.SomeClass
            import b.SomeClass as B

            public object SomeConverterImpl : SomeConverter {
              override fun toB(source: SomeClass): B? = B().also { someClass ->
                someClass.property = source.property
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun handleSameClassNameInDifferentPackagesWithSourceImportAliasWithNullable() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
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
import a.SomeClass as A
import b.SomeClass

@Konverter
interface SomeConverter {
    @Konvert
    fun toSomeClass(source: A?): SomeClass?
}
                    """.trimIndent()
                )
            )
        )
        val mapperCode = compilation.generatedSourceFor("SomeConverterKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import b.SomeClass
            import a.SomeClass as A

            public object SomeConverterImpl : SomeConverter {
              override fun toSomeClass(source: A?): SomeClass? = source?.let {
                SomeClass().also { someClass ->
                  someClass.property = source.property
                }
              }
            }
            """.trimIndent(),
            mapperCode
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
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = if (globalSuffix != null) {
                mapOf(GENERATED_FILENAME_SUFFIX_OPTION.key to globalSuffix)
            } else {
                mapOf()
            },
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents = // @formatter:off
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konfig

${if (localSuffix != null) {
    """@Konverter(options=[Konfig(key="${GENERATED_FILENAME_SUFFIX_OPTION.key}", value="$localSuffix")])"""
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
        val mapperCode = compilation.generatedSourceFor("MyMapper${expectedSuffix}.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MyMapperImpl : MyMapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun callThisWhenInsideSameKonverter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: SourceProperty)
class TargetClass(val property: TargetProperty)
data class SourceProperty(val prop: String)
data class TargetProperty(val prop: String)

@Konverter
interface Mapper {
    fun toTarget(sourceClass: SourceClass): TargetClass
    fun toTarget(source: SourceProperty): TargetProperty
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(sourceClass: SourceClass): TargetClass = TargetClass(
                property = this.toTarget(source = sourceClass.property)
              )

              override fun toTarget(source: SourceProperty): TargetProperty = TargetProperty(
                prop = source.prop
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun recursiveTreeMap() {
        val (compilation) = compileWith(
            enabledConverters = listOf(IterableToIterableConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
}

class SourceClass(val children: List<SourceClass>)
class TargetClass(val children: List<TargetClass>)
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                children = source.children.map { this.toTarget(source = it) }
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun useOtherMapperInDifferentPackage() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IntToStringConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "a/TestCode.kt",
                    contents =
                    """
package a

import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping
import b.SourceProperty
import b.TargetProperty

class SourceClass(val sourceProperty: SourceProperty<Int>)
class TargetClass(val targetProperty: TargetProperty<String>)

@Konverter
interface ClassMapper {
    @Konvert(mappings=[Mapping(source="sourceProperty", target="targetProperty")])
    fun toTarget(source: SourceClass): TargetClass
}
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/TestCode.kt",
                    contents =
                    """
package b

import io.mcarle.konvert.api.Konverter

@Konverter
interface PropertyMapper {
    fun toTarget(source: SourceProperty<Int>): TargetProperty<String> = TargetProperty("${'$'}{source.value}")
}

class SourceProperty<E>(val value: E)
class TargetProperty<E>(val value: E)
                    """.trimIndent()
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("ClassMapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            package a

            import b.PropertyMapper
            import io.mcarle.konvert.api.Konverter

            public object ClassMapperImpl : ClassMapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                targetProperty = Konverter.get<PropertyMapper>().toTarget(source = source.sourceProperty)
              )
            }
        """.trimIndent(), extensionFunctionCode
        )
    }

    @Test
    fun useIterableTypeConverter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToIterableConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    contents =
                    """
import io.mcarle.konvert.api.Konverter
import java.util.ArrayList

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
    fun toTargetList(source: List<SourceClass>): List<TargetClass>
    fun toTargetSet(source: List<SourceClass>): Set<TargetClass>
    fun toTargetArrayList(source: Iterable<SourceClass>): ArrayList<TargetClass>
}
                    """.trimIndent(),
                    name = "TestCode.kt"
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import java.util.ArrayList
            import kotlin.collections.Iterable
            import kotlin.collections.List
            import kotlin.collections.Set

            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )

              override fun toTargetList(source: List<SourceClass>): List<TargetClass> = source.map {
                  this.toTarget(source = it) }

              override fun toTargetSet(source: List<SourceClass>): Set<TargetClass> = source.map {
                  this.toTarget(source = it) }.toSet()

              override fun toTargetArrayList(source: Iterable<SourceClass>): ArrayList<TargetClass> =
                  source.map { this.toTarget(source = it) }.toCollection(kotlin.collections.ArrayList())
            }
        """.trimIndent(), extensionFunctionCode
        )
    }

    @Test
    fun ignorePrivateFunctions() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    contents =
                    """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass

    private fun ignoreMe(source: SourceClass): TargetClass = toTarget(source)
}
                    """.trimIndent(),
                    name = "TestCode.kt"
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
        """.trimIndent(), extensionFunctionCode
        )
    }

    @Test
    fun ignoreExtensionFunctions() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    contents =
                    """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass

    fun SourceClass.ignoreMe(source: SourceClass): TargetClass = toTarget(source)
}
                    """.trimIndent(),
                    name = "TestCode.kt"
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
        """.trimIndent(), extensionFunctionCode
        )
    }

    @Test
    fun ignoreFunctionsWithUnitReturnType() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    contents =
                    """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
    fun ignoreMe(source: SourceClass) {}
}
                    """.trimIndent(),
                    name = "TestCode.kt"
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
        """.trimIndent(), extensionFunctionCode
        )
    }

    @Test
    fun allowMultipleFunctionParametersIfOneIsAnnotatedWithSource() {
        addGeneratedKonverterAnnotation = true // enable to verify, that no annotation is generated
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    contents =
                    """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String, val otherValue: Int)

@Konverter
interface Mapper {
    fun toTarget(@Konverter.Source source: SourceClass, otherValue: Int, vararg furtherParams: String): TargetClass
}
                    """.trimIndent(),
                    name = "TestCode.kt"
                )
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertFalse { extensionFunctionCode.contains("@GeneratedKonverter") }

        assertSourceEquals(
            """
            import kotlin.Int
            import kotlin.String

            public object MapperImpl : Mapper {
              override fun toTarget(
                source: SourceClass,
                otherValue: Int,
                vararg furtherParams: String,
              ): TargetClass = TargetClass(
                property = source.property,
                otherValue = otherValue
              )
            }
        """.trimIndent(), extensionFunctionCode
        )
    }

    @Test
    fun allowMultipleFunctionParametersIfOneIsAnnotatedWithSourceInSelfImplementedMappingFunctions() {
        addGeneratedKonverterAnnotation = true // enable to verify, that no annotation is generated
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    contents =
                    """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String, val otherValue: Int)

@Konverter
interface Mapper {
    fun toTarget(@Konverter.Source source: SourceClass, otherValue: Int, vararg furtherParams: String): TargetClass = TargetClass(
      property = source.property,
      otherValue = otherValue
    )
}
                    """.trimIndent(),
                    name = "TestCode.kt"
                )
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertFalse { mapperCode.contains("@GeneratedKonverter") }

        assertSourceEquals(
            """
            import kotlin.Int
            import kotlin.String

            public object MapperImpl : Mapper {
              override fun toTarget(
                source: SourceClass,
                otherValue: Int,
                vararg furtherParams: String,
              ): TargetClass = super.toTarget(
                  source = source,
                  otherValue = otherValue,
                  furtherParams = furtherParams
              )
            }
        """.trimIndent(), mapperCode
        )
    }

}
