package io.mcarle.konvert.processor.konvert

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.config.KONVERTER_GENERATE_CLASS_OPTION
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

@OptIn(ExperimentalCompilerApi::class)
class SealedKonverterITest : KonverterITest() {

    @Test
    fun mapSealedInterfaceToSealedInterface() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    data class B(val number: Int) : Source
    data class A(val value: String) : Source
}
sealed interface Target {
    data class A(val value: String) : Target
    data class B(val number: Int) : Target
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: Source): Target = when (source) {
                is Source.A -> Target.A(
                  value = source.value
                )
                is Source.B -> Target.B(
                  number = source.number
                )
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun mapObjectsAndClassesMixed() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    data object Empty : Source
    data class Text(val value: String) : Source
    data class Gone(val reason: String) : Source
}
sealed interface Target {
    data object Empty : Target
    data class Text(val value: String) : Target
    data object Gone : Target
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: Source): Target = when (source) {
                is Source.Empty -> Target.Empty
                is Source.Gone -> Target.Gone
                is Source.Text -> Target.Text(
                  value = source.value
                )
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun reuseKonverterFunctionOfSubclassPair() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

sealed interface Source {
    data class A(val value: String) : Source
    data class B(val number: Int) : Source
}
sealed interface Target {
    data class A(val text: String) : Target
    data class B(val number: Int) : Target
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target

    @Konvert(mappings = [Mapping(source = "value", target = "text")])
    fun toTargetA(source: Source.A): Target.A

    fun toTargetB(source: Source.B): Target.B = Target.B(source.number * 2)
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "is Source.A -> this.toTargetA(source = source)")
        assertContains(mapperCode, "is Source.B -> this.toTargetB(source = source)")
    }

    @Test
    fun flattenNestedSealedHierarchy() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    sealed interface Shape : Source {
        data class Circle(val radius: Int) : Shape
        data object Dot : Shape
    }
    data class Text(val value: String) : Source
}
sealed interface Target {
    sealed interface Shape : Target {
        data class Circle(val radius: Int) : Shape
        data object Dot : Shape
    }
    data class Text(val value: String) : Target
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: Source): Target = when (source) {
                is Source.Shape.Circle -> Target.Shape.Circle(
                  radius = source.radius
                )
                is Source.Shape.Dot -> Target.Shape.Dot
                is Source.Text -> Target.Text(
                  value = source.value
                )
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun nestedSealedPairPrefersItsOwnKonverterFunction() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    sealed interface Shape : Source {
        data class Circle(val radius: Int) : Shape
    }
    data class Text(val value: String) : Source
}
sealed interface Target {
    sealed interface Shape : Target {
        data class Circle(val radius: Int) : Shape
    }
    data class Text(val value: String) : Target
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
    fun toShape(source: Source.Shape): Target.Shape
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "is Source.Shape -> this.toShape(source = source)")
    }

    @Test
    fun mapSealedClassWithAbstractProperty() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed class Source {
    abstract val id: Int
    data class A(override val id: Int, val name: String) : Source()
    class B(override val id: Int) : Source()
}
sealed class Target {
    abstract val id: Int
    data class A(override val id: Int, val name: String) : Target()
    class B(override val id: Int) : Target()
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: Source): Target = when (source) {
                is Source.A -> Target.A(
                  id = source.id,
                  name = source.name
                )
                is Source.B -> Target.B(
                  id = source.id
                )
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun sealedPropertyInsideClassUsesGeneratedFunction() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    data class A(val value: String) : Source
}
sealed interface Target {
    data class A(val value: String) : Target
}
class Wrapper(val payload: Source)
class WrapperDto(val payload: Target)

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
    fun toDto(source: Wrapper): WrapperDto
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "payload = this.toTarget(source = source.payload)")
    }

    @Test
    fun nullableSourceAndTarget() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    data class A(val value: String) : Source
}
sealed interface Target {
    data class A(val value: String) : Target
}

@Konverter
interface Mapper {
    fun toTarget(source: Source?): Target?
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: Source?): Target? = when (source) {
                null -> null
                is Source.A -> Target.A(
                  value = source.value
                )
              }
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun pairSubclassesWithDifferentNamesThroughKonverterFunction() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface PricingPayload {
    val type: String
}
data class LinearPricingPayload(override val type: String, val price: Int) : PricingPayload
data class PerWinPricingPayload(override val type: String, val pricePerWin: Int) : PricingPayload

sealed interface PricingData
data class LinearData(val price: Int) : PricingData
data class PerWinData(val pricePerWin: Int) : PricingData

@Konverter
interface Mapper {
    fun toData(source: PricingPayload): PricingData
    fun toData(source: LinearPricingPayload): LinearData
    fun toData(source: PerWinPricingPayload): PerWinData
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(
            mapperCode,
            """
              override fun toData(source: PricingPayload): PricingData = when (source) {
                is LinearPricingPayload -> this.toData(source = source)
                is PerWinPricingPayload -> this.toData(source = source)
              }
            """.trimIndent().prependIndent("  ")
        )
    }

    @Test
    fun konverterFunctionWinsOverSameNamedSubclass() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    data class A(val value: String) : Source
}
sealed interface Target {
    data class A(val value: String) : Target
    data class Legacy(val value: String) : Target
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
    fun toLegacy(source: Source.A): Target.Legacy
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "is Source.A -> this.toLegacy(source = source)")
    }

    @Test
    fun mapEverySubclassOntoOneTargetThroughItsOwnFunction() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

sealed interface Widget {
    data class Slider(val min: Int, val max: Int) : Widget
    data class Select(val options: List<String>) : Widget
}
data class FlatWidget(val min: Int? = null, val max: Int? = null, val options: List<String> = emptyList())

@Konverter
interface Mapper {
    fun toFlat(source: Widget): FlatWidget

    @Konvert(mappings = [Mapping(target = "options", ignore = true)])
    fun toFlat(source: Widget.Slider): FlatWidget

    @Konvert(mappings = [Mapping(target = "min", ignore = true), Mapping(target = "max", ignore = true)])
    fun toFlat(source: Widget.Select): FlatWidget
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(
            mapperCode,
            """
              override fun toFlat(source: Widget): FlatWidget = when (source) {
                is Widget.Select -> this.toFlat(source = source)
                is Widget.Slider -> this.toFlat(source = source)
              }
            """.trimIndent().prependIndent("  ")
        )
    }

    @Test
    fun failOnSourceSubclassWithoutTargetPair() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    data class A(val value: String) : Source
    data class B(val value: String) : Source
    data object C : Source
}
sealed interface Target {
    data class A(val value: String) : Target
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
                """.trimIndent()
            )
        )

        assertContains(result.messages, "No subclass of Target has the simple name of, or a converter from, [Source.B, Source.C]")
    }

    @Test
    fun failOnAmbiguousTargetPair() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    data class A(val value: String) : Source
}
sealed interface Target {
    data class A(val value: String) : Target
}
data class A(val value: String) : Target

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
                """.trimIndent()
            )
        )

        assertContains(result.messages, "Source.A matches more than one subclass of Target: [A, Target.A]")
    }

    @Test
    fun failOnGenericSealedType() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source<T> {
    data class A<T>(val value: T) : Source<T>
}
sealed interface Target<T> {
    data class A<T>(val value: T) : Target<T>
}

@Konverter
interface Mapper {
    fun toTarget(source: Source<String>): Target<String>
}
                """.trimIndent()
            )
        )

        assertContains(result.messages, "Mapping generic sealed types is not supported: Source -> Target")
    }

    @Test
    fun failOnMappingsForSealedTypes() {
        val (_, result) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            expectResultCode = KotlinCompilation.ExitCode.INTERNAL_ERROR,
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

sealed interface Source {
    data class A(val value: String) : Source
}
sealed interface Target {
    data class A(val text: String) : Target
}

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(source = "value", target = "text")])
    fun toTarget(source: Source): Target
}
                """.trimIndent()
            )
        )

        assertContains(
            result.messages,
            "Mappings and constructor selection are not supported between sealed types (Source -> Target), declare a function per subclass pair instead"
        )
    }

    @Test
    fun generateClassInsteadOfObject() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            options = mapOf(KONVERTER_GENERATE_CLASS_OPTION.key to "true"),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    data class A(val value: String) : Source
}
sealed interface Target {
    data class A(val value: String) : Target
}

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "public class MapperImpl : Mapper {")
        assertContains(mapperCode, "is Source.A -> Target.A(")
    }

    @Test
    fun sealedSourceToNonSealedTargetKeepsPropertyMapping() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    val id: Int
    data class A(override val id: Int) : Source
}
class Target(val id: Int)

@Konverter
interface Mapper {
    fun toTarget(source: Source): Target
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: Source): Target = Target(
                id = source.id
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun sameSealedTypeKeepsSameTypeConverter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

sealed interface Source {
    data class A(val value: String) : Source
}

@Konverter
interface Mapper {
    fun copy(source: Source): Source
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "override fun copy(source: Source): Source = source")
    }
}
