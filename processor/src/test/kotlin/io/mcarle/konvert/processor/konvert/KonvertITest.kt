package io.mcarle.konvert.processor.konvert

import com.squareup.kotlinpoet.ksp.toClassName
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.DEFAULT_KONVERTER_PRIORITY
import io.mcarle.konvert.api.DEFAULT_KONVERT_PRIORITY
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.converter.IntToStringConverter
import io.mcarle.konvert.converter.IterableToArrayListConverter
import io.mcarle.konvert.converter.IterableToListConverter
import io.mcarle.konvert.converter.IterableToSetConverter
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.GENERATED_FILENAME_SUFFIX_OPTION
import io.mcarle.konvert.converter.api.config.KONVERTER_GENERATE_CLASS_OPTION
import io.mcarle.konvert.converter.api.config.KONVERTER_USE_REFLECTION_OPTION
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCompilerApi::class)
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
        assertEquals("Mapper", converter.konverterInterface.simpleName)
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
        assertEquals("Mapper", converter.konverterInterface.simpleName)
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
        assertEquals("Mapper", converter.konverterInterface.simpleName)
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

        assertContains(mapperCode, "OtherMapperImpl.toTarget(")
    }

    @Test
    fun useOtherMapperInstance() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(KONVERTER_GENERATE_CLASS_OPTION.key to "true"),
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

        assertContains(mapperCode, "OtherMapperImpl().toTarget(")
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

        assertContains(mapperCode, "OtherMapperImpl.toInt(")
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

        assertContains(mapperCode, "OtherMapperImpl.toListOfInts(")
        assertContains(mapperCode, "OtherMapperImpl.optionalToListOfInts(")
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

        // Verify that the generated class is not an object
        assertNull(result.classLoader.loadClass("SomeConverterImpl").kotlin.objectInstance)
        assertNotNull(result.classLoader.loadClass("SomeConverterImpl").constructors.first().newInstance())

        // Verify that the generated class can be loaded correctly via reflection
        assertNotNull(Konverter.getWithClassLoader("SomeConverter", result.classLoader))
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
              override fun toSomeClass(source: ASomeClass): BSomeClass = BSomeClass().also { someClass ->
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
              override fun toSomeClass(source: ASomeClass): BSomeClass? = BSomeClass().also { someClass ->
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

        // TODO: use alias in return type after https://github.com/square/kotlinpoet/issues/2020 is solved
        assertSourceEquals(
            """
            import a.SomeClass
            import b.SomeClass as B

            public object SomeConverterImpl : SomeConverter {
              override fun toB(source: SomeClass): b.SomeClass? = B().also { someClass ->
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

        // TODO: use alias in function parameter after https://github.com/square/kotlinpoet/issues/2020 is solved
        assertSourceEquals(
            """
            import b.SomeClass
            import a.SomeClass as A

            public object SomeConverterImpl : SomeConverter {
              override fun toSomeClass(source: a.SomeClass?): SomeClass? = source?.let {
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
            enabledConverters = listOf(IterableToListConverter()),
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
        val mapperCode = compilation.generatedSourceFor("ClassMapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            package a

            import b.PropertyMapperImpl

            public object ClassMapperImpl : ClassMapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                targetProperty = PropertyMapperImpl.toTarget(source = source.sourceProperty)
              )
            }
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun useIterableTypeConverter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(
                SameTypeConverter(),
                IterableToListConverter(),
                IterableToSetConverter(),
                IterableToArrayListConverter()
            ),
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
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

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

              override fun toTargetList(source: List<SourceClass>): List<TargetClass> = source.map { this.toTarget(source = it) }

              override fun toTargetSet(source: List<SourceClass>): Set<TargetClass> = source.map { this.toTarget(source = it) }.toSet()

              override fun toTargetArrayList(source: Iterable<SourceClass>): ArrayList<TargetClass> = source.map { this.toTarget(source = it) }.toCollection(kotlin.collections.ArrayList())
            }
        """.trimIndent(), mapperCode
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
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
        """.trimIndent(), mapperCode
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
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
        """.trimIndent(), mapperCode
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
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun failOnAbstractFunctionsAboutMultipleFunctionParametersIfNoneIsAnnotatedWithSource() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.COMPILATION_ERROR,
            code = arrayOf(
                SourceFile.kotlin(
                    contents =
                        """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String, val otherValue: Int)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass, otherValue: Int): TargetClass
}
                    """.trimIndent(),
                    name = "TestCode.kt"
                )
            )
        )

        assertContains(
            compilationResult.messages,
            "Konvert annotated function must have exactly one source parameter (either single parameter or annotated with @Konverter.Source) and must have a return type: Mapper.toTarget"
        )
    }

    @Test
    fun complainOnImplementedAnnotatedFunctionsIfSourceAndOrTargetCouldNotBeDetermined() {
        val (_, compilationResult) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK, // should compile, but with warning
            code = arrayOf(
                SourceFile.kotlin(
                    contents =
                        """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert

class SourceClass(val property: String)
class TargetClass(val property: String, val otherValue: Int)

@Konverter
interface Mapper {
    @Konvert
    fun toTarget(source: SourceClass, otherValue: Int): TargetClass = TargetClass(source.property, otherValue)
}
                    """.trimIndent(),
                    name = "TestCode.kt"
                )
            )
        )

        assertContains(
            compilationResult.messages,
            "Ignoring annotated implemented function as source and/or target could not be determined"
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
              ): TargetClass = TargetClass(
                property = source.property,
                otherValue = otherValue
              )
            }
        """.trimIndent(), mapperCode
        )
    }

    @Test
    fun additionalFunctionParametersTakePrecedenceOverSourceValue() {
        addGeneratedKonverterAnnotation = true // enable to verify, that no annotation is generated
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    contents =
                        """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String, val otherValue: Int)
class TargetClass(val property: String, val otherValue: Int)

@Konverter
interface Mapper {
    fun toTarget(@Konverter.Source source: SourceClass, otherValue: Int): TargetClass
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

            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass, otherValue: Int): TargetClass = TargetClass(
                property = source.property,
                otherValue = otherValue
              )
            }
        """.trimIndent(), mapperCode
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

    @Test
    fun useOtherMapperViaReflection() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(KONVERTER_USE_REFLECTION_OPTION.key to "true"),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: SourceProperty)
class TargetClass(val property: TargetProperty)

@Konverter
interface Mapper {
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
    fun nestedClass() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.OK,
            code = arrayOf(
                SourceFile.kotlin(
                    name = "a/Person.kt",
                    contents =
                        """
package a

data class Person(val firstName: String, val lastName: String, val age: Int, val address: Address) {
    data class Address(val address1: String, val address2: String)
}
                """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/PersonDto.kt",
                    contents =
                        """
package b

data class PersonDto(val firstName: String, val lastName: String, val age: Int, val address: AddressDto) {
    data class AddressDto(val address1: String, val address2: String)
}
                """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "c/DomainMapper.kt",
                    contents =
                        """
package c

import io.mcarle.konvert.api.Konverter
import a.Person
import b.PersonDto

@Konverter
interface DomainMapper {
    fun toAddressDto(address: Person.Address): PersonDto.AddressDto
}
                """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "d/DtoMapper.kt",
                    contents =
                        """
package d

import io.mcarle.konvert.api.Konverter
import a.Person.Address as AddressDomain
import b.PersonDto

@Konverter
interface DtoMapper {
    fun toAddress(address: PersonDto.AddressDto): AddressDomain
}
                """.trimIndent()
                ),
            )
        )
        val domainMapperCode = compilation.generatedSourceFor("DomainMapperKonverter.kt")
        println(domainMapperCode)
        val dtoMapperCode = compilation.generatedSourceFor("DtoMapperKonverter.kt")
        println(dtoMapperCode)

        assertSourceEquals(
            """
            package c

            import a.Person
            import b.PersonDto

            public object DomainMapperImpl : DomainMapper {
              override fun toAddressDto(address: Person.Address): PersonDto.AddressDto = PersonDto.AddressDto(
                address1 = address.address1,
                address2 = address.address2
              )
            }
            """.trimIndent(),
            domainMapperCode
        )
        assertSourceEquals(
            """
            package d

            import b.PersonDto
            import a.Person.Address as AddressDomain

            public object DtoMapperImpl : DtoMapper {
              override fun toAddress(address: PersonDto.AddressDto): AddressDomain = AddressDomain(
                address1 = address.address1,
                address2 = address.address2
              )
            }
            """.trimIndent(),
            dtoMapperCode
        )
    }

    @Test
    fun suspendFun() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: SourceProperty)
class TargetClass(val property: TargetProperty)

class SourceProperty(val value: String)
class TargetProperty(val value: String)

@Konverter
interface Mapper {
    suspend fun toTarget(source: SourceClass): TargetClass
    suspend fun toTarget(source: SourceProperty): TargetProperty =
        TargetProperty(source.value)
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "suspend fun toTarget(source: SourceClass): TargetClass")
        assertContains(mapperCode, "suspend fun toTarget(source: SourceProperty): TargetProperty")
    }

    @Test
    fun internalInterface() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
internal interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "internal object MapperImpl : Mapper {")
    }
}

private fun Konverter.Companion.getWithClassLoader(classFQN: String, classLoader: ClassLoader): Any {
    try {
        addClassLoader(classLoader)
        return get(classLoader.loadClass(classFQN).kotlin)
    } finally {
        removeClassLoader(classLoader)
    }
}
