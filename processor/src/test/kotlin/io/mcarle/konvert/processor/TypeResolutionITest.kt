package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.IntToStringConverter
import io.mcarle.konvert.converter.IterableToListConverter
import io.mcarle.konvert.converter.IterableToSetConverter
import io.mcarle.konvert.converter.MapToMapConverter
import io.mcarle.konvert.converter.SameTypeConverter
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * Tests for the type resolution (see `io.mcarle.konvert.processor.TypeResolution`), which is responsible for
 * expanding typealiases (also nested ones inside type arguments) and for replacing type parameters with their
 * matching type arguments.
 */
@Suppress("RedundantVisibilityModifier")
@OptIn(ExperimentalCompilerApi::class)
class TypeResolutionITest : KonverterITest() {

    @Test
    fun resolveChainedTypealiasesOnSourceProperty() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag(val id: Int)
typealias SourceTags<T> = List<T>
typealias SourceTagsView = SourceTags<STag>
typealias STag = SourceTag
data class SourceView(val tags: SourceTagsView)

data class TargetTag(val id: Int)
data class TargetView(val tags: List<TargetTag>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "source.tags.map { this.toTag(source = it) }")
    }

    @Test
    fun resolveDeeplyNestedTypealiasesToMatchKonverterOfOtherInterface() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

typealias SourceMap<X, E> = Map<E, X>

typealias SourceAlias<A, B> = SourceTag<B, A>
data class SourceTag<X, T>(val id: SourceMap<T, X>, val side: X)
data class TargetTag<T>(val id: T)

@Konverter
interface Mapper {
    fun toTag(source: SourceAlias<SourceMap<String, SourceMap<Int, List<SourceMap<Int, String>>>>, Int>): TargetTag<String>
    // source.id resolves to: Map<Int, Map<Map<List<Map<String, Int>>, Int>, String>>
}

@Konverter
interface Mapper2 {
    fun holdMyBeer(source: Map<Int, Map<Map<List<Map<String, Int>>, Int>, String>>): String {
        return source.toString()
    }
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "id = Mapper2Impl.holdMyBeer(source = source.id)")
    }

    @Test
    fun resolveChainedGenericTypealiasesWithPropagatedTypeArgument() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IntToStringConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T>(val id: T)
data class TargetTag<T>(val id: T)

typealias Tags<T> = List<T>
typealias SourceTagsView<T> = Tags<SourceTag<T>>
typealias IntSourceTagsView = SourceTagsView<Int>

data class SourceView(val tags: IntSourceTagsView)
data class TargetView(val tags: List<TargetTag<String>>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<Int>): TargetTag<String>
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "source.tags.map { this.toTag(source = it) }")
    }

    @Test
    fun resolveChainedGenericTypealiasesWithPartiallyFixedTypeArguments() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IntToStringConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T, X>(val id: T, val bla: X)
data class TargetTag<T>(val id: T)

typealias Tags<T> = List<T>
typealias SourceTagsView<T> = Tags<SourceTag<T, String>>
typealias IntSourceTagsView = SourceTagsView<Int>

data class SourceView(val tags: IntSourceTagsView)
data class TargetView(val tags: List<TargetTag<String>>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<Int, String>): TargetTag<String>
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "source.tags.map { this.toTag(source = it) }")
    }

    @Test
    fun resolveTypealiasesWithAllCombinationsOfFixedAndGenericTypeArguments() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IntToStringConverter(), IterableToListConverter(), IterableToSetConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T, X>(val id: T, val bla: X)
data class TargetTag<T>(val id: T, val bla: String)

// A: all type arguments fixed
typealias IntSourceTagString = SourceTag<Int, String>

// B: first type argument generic, second fixed
typealias CustomSourceTagString<T> = SourceTag<T, String>

// C: first type argument fixed, second generic
typealias IntSourceTagCustom<T> = SourceTag<Int, T>

// D: all type arguments generic
typealias CustomSourceTagCustom<T, X> = SourceTag<T, X>

data class SourceViewA(val tags: IntSourceTagString)
data class SourceViewB(val tags: CustomSourceTagString<Int>)
data class SourceViewC(val tags: IntSourceTagCustom<String>)
data class SourceViewD(val tags: CustomSourceTagCustom<Int, String>)

data class TargetView(val tags: TargetTag<String>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<Int, String>): TargetTag<String>

    fun toViewA(source: SourceViewA): TargetView
    fun toViewB(source: SourceViewB): TargetView
    fun toViewC(source: SourceViewC): TargetView
    fun toViewD(source: SourceViewD): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "override fun toViewA(source: SourceViewA): TargetView = TargetView(")
        assertContains(mapperCode, "override fun toViewB(source: SourceViewB): TargetView = TargetView(")
        assertContains(mapperCode, "override fun toViewC(source: SourceViewC): TargetView = TargetView(")
        assertContains(mapperCode, "override fun toViewD(source: SourceViewD): TargetView = TargetView(")
        assertEquals(4, mapperCode.split("tags = this.toTag(source = source.tags)").size - 1)
    }

    @Test
    fun resolveGenericTypealiasWithNestedGenericType() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IntToStringConverter(), IterableToListConverter(), IterableToSetConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T>(val id: T)
data class TargetTag(val id: String)

typealias SourceTagsView<T> = List<SourceTag<T>>

data class SourceView(val tags: SourceTagsView<Int>)
data class TargetView(val tags: List<TargetTag>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<Int>): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "source.tags.map { this.toTag(source = it) }")
    }

    @Test
    fun resolveTypealiasesWithSwappedTypeArgumentsAndImportAliasesInDifferentFiles() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), MapToMapConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "a/SourceTag.kt",
                    contents =
                        """
package a

data class SourceTag<T>(val id: T)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "TestCode.kt",
                    contents =
                        """
import a.SourceTag as STag

data class TargetTag(val id: String)

typealias SourceTagsView<T, E> = Map<T, STag<E>>
typealias SwappedSourceTagsView<T, E> = SourceTagsView<E, T>

data class SourceView(val tags: SwappedSourceTagsView<String, Int>)
data class TargetView(val tags: Map<Int, TargetTag>)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "Mapper.kt",
                    contents =
                        """
import io.mcarle.konvert.api.Konverter
import a.SourceTag as SourceT

@Konverter
interface Mapper {
    fun toTag(source: SourceT<String>): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
                ),
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "source.tags.mapValues { (_, it) -> this.toTag(source = it) }")
    }

    @Test
    fun resolveTypealiasesWithReorderedAndNestedGenerics() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<X, T>(val id: Map<T, X>, val side: X)
data class TargetTag(val id: Map<String, Int>, val side: Int)

typealias Swapped<A, B> = SourceTag<B, A>
typealias Tags<T> = List<T>
typealias SwappedTags<A, B> = Tags<Swapped<A, B>>

data class SourceView(val tags: SwappedTags<String, Int>, val optionalTags: SwappedTags<String, Int>?)
data class TargetView(val tags: List<TargetTag>, val optionalTags: List<TargetTag>?)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<Int, String>): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertSourceEquals(
            """
            import kotlin.Int
            import kotlin.String

            public object MapperImpl : Mapper {
              override fun toTag(source: SourceTag<Int, String>): TargetTag = TargetTag(
                id = source.id,
                side = source.side
              )

              override fun toView(source: SourceView): TargetView = TargetView(
                tags = source.tags.map { this.toTag(source = it) },
                optionalTags = source.optionalTags?.map { this.toTag(source = it) }
              )
            }
            """.trimIndent(),
            mapperCode
        )
    }

    @Test
    fun resolveNullableTypealias() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag(val id: Int)
data class TargetTag(val id: Int)

typealias OptionalSourceTag = SourceTag?

data class SourceView(val tag: OptionalSourceTag)
data class TargetView(val tag: TargetTag?)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "tag = source.tag?.let { this.toTag(source = it) }")
    }

    @Test
    fun resolveTypealiasesUsedAsKonverterSourceAndTarget() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag(val id: Int)
data class TargetTag(val id: Int)
data class SourceView(val tags: List<SourceTag>)
data class TargetView(val tags: List<TargetTag>)

typealias Source = SourceTag
typealias Target = TargetTag

@Konverter
interface Mapper {
    fun toTag(source: Source): Target
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "source.tags.map { this.toTag(source = it) }")
    }

    @Test
    fun keepStarProjectionWhenResolvingTypealias() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T>(val id: T)
data class TargetTag(val id: String)

typealias Tags<T> = List<T>
typealias StarSourceTags = Tags<SourceTag<*>>

data class SourceView(val tags: StarSourceTags)
data class TargetView(val tags: List<TargetTag>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<*>): TargetTag = TargetTag(source.id.toString())
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "source.tags.map { this.toTag(source = it) }")
    }

    @Test
    fun keepStarProjectionWhenItIsBoundToTypeParameterOfTypealias() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToSetConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

typealias Tags<T> = List<T>
typealias Boxes<T> = Tags<T>
typealias StarTags = Tags<*>

data class SourceView(
    val aliasTags: Tags<*>,
    val nestedAliasTags: Boxes<*>,
    val chainedTags: StarTags,
    val directTags: List<*>
)

data class TargetView(
    val aliasTags: Set<Any?>,
    val nestedAliasTags: Set<Any?>,
    val chainedTags: Set<Any?>,
    val directTags: Set<Any?>
)

@Konverter
interface Mapper {
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        // all properties are the very same type (List<*>) and must therefore be converted identically
        assertContains(mapperCode, "aliasTags = source.aliasTags.toSet()")
        assertContains(mapperCode, "nestedAliasTags = source.nestedAliasTags.toSet()")
        assertContains(mapperCode, "chainedTags = source.chainedTags.toSet()")
        assertContains(mapperCode, "directTags = source.directTags.toSet()")
    }

    @Test
    fun keepStarProjectionWhenItIsBoundToNestedTypeParameterOfTypealias() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T>(val id: T)
data class TargetTag(val id: String)

typealias Tags<T> = List<SourceTag<T>>

data class SourceView(val tags: Tags<*>)
data class TargetView(val tags: List<TargetTag>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<*>): TargetTag = TargetTag(source.id.toString())
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        // Tags<*> resolves to List<SourceTag<*>>
        assertContains(mapperCode, "tags = source.tags.map { this.toTag(source = it) }")
    }

    @Test
    fun keepStarProjectionWhenItIsBoundToTypeParameterOfGenericSourceClass() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter(), IterableToSetConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T>(val id: T)
data class TargetTag(val id: String)

typealias Tags<T> = List<T>

data class SourceView<T>(val tags: Tags<SourceTag<T>>, val plainTags: Tags<T>)
data class TargetView(val tags: List<TargetTag>, val plainTags: Set<Any?>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<*>): TargetTag = TargetTag(source.id.toString())
    fun toView(source: SourceView<*>): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "tags = source.tags.map { this.toTag(source = it) }")
        assertContains(mapperCode, "plainTags = source.plainTags.toSet()")
    }

    @Test
    fun resolveTypeParametersOfGenericSourceClass() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T>(val id: T)
data class SourceView<T>(val tags: List<SourceTag<T>>)

data class TargetTag(val id: Int)
data class TargetView(val tags: List<TargetTag>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<Int>): TargetTag
    fun toView(source: SourceView<Int>): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "source.tags.map { this.toTag(source = it) }")
    }

    /**
     * Regression test combining all aspects of the type resolution in a single mapping:
     * * chained typealiases (`OptionalSwappedTags` -> `Tags` -> `Swapped` -> `SourceTag`)
     * * swapped type arguments
     * * typealiases (and import aliases of typealiases) used as type arguments
     * * nullability on the typealias itself, on the properties and on the konverter function
     * * import aliases spread over multiple files and packages
     */
    @Test
    fun resolveNullableChainedTypealiasesWithSwappedTypealiasTypeArgumentsAndImportAliases() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "a/SourceTag.kt",
                    contents =
                        """
package a

data class SourceTag<X, T>(val id: Map<T, X>, val side: X)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "b/TargetTag.kt",
                    contents =
                        """
package b

data class TargetTag(val id: Map<String, Int>, val side: Int)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "c/Aliases.kt",
                    contents =
                        """
package c

typealias Id = String
typealias Side = Int
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "TestCode.kt",
                    contents =
                        """
import a.SourceTag as STag
import b.TargetTag as TTag
import c.Id
import c.Side as S

typealias Swapped<A, B> = STag<B, A>
typealias Tags<T> = List<T>
typealias OptionalSwappedTags<A, B> = Tags<Swapped<A, B>>?

data class SourceView(val tags: OptionalSwappedTags<Id, S>, val tag: Swapped<Id, S>?)
data class TargetView(val tags: List<TTag>?, val tag: TTag?)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "Mapper.kt",
                    contents =
                        """
import io.mcarle.konvert.api.Konverter
import a.SourceTag as SourceT
import b.TargetTag as TargetT

@Konverter
interface Mapper {
    fun toTag(source: SourceT<Int, String>): TargetT
    fun toView(source: SourceView?): TargetView?
}
                    """.trimIndent()
                ),
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        // the import alias of the return type is kept (the one of the source parameter is lost, see KonverterCodeGenerator.isAlias)
        assertContains(mapperCode, "import b.TargetTag as TargetT")
        assertContains(mapperCode, "tags = source.tags?.map { this.toTag(source = it) }")
        assertContains(mapperCode, "tag = source.tag?.let { this.toTag(source = it) }")
    }

    /**
     * The type parameter names of the typealias are intentionally the same as the ones of the class, but
     * bound to the other type argument. Therefore, they must not be mixed up during the resolution.
     */
    @Test
    fun resolveTypealiasWithTypeParameterNamesShadowingTheOnesOfTheClass() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T, X>(val id: T, val side: X)
data class TargetTag(val id: String, val side: Int)

typealias Shadowed<X, T> = SourceTag<T, X>

data class SourceView(val tag: Shadowed<Int, String>)
data class TargetView(val tag: TargetTag)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<String, Int>): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "tag = this.toTag(source = source.tag)")
    }

    @Test
    fun resolveNullableTypealiasUsedAsTypeArgumentOfAnotherTypealias() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag(val id: Int)
data class TargetTag(val id: Int)

typealias OptionalSourceTag = SourceTag?
typealias Tags<T> = List<T>
typealias OptionalSourceTags = Tags<OptionalSourceTag>
typealias AliasOfAlias = OptionalSourceTags

data class SourceView(val tags: AliasOfAlias)
data class TargetView(val tags: List<TargetTag?>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "tags = source.tags.map { it?.let { this.toTag(source = it) } }")
    }

    @Test
    fun resolveTypealiasWithRepeatedTypeParameter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<A, B>(val first: A, val second: B)
data class TargetTag(val first: Int, val second: Int)

typealias Duplicated<T> = SourceTag<T, T>
typealias DuplicatedTags<T> = List<Duplicated<T>>

data class SourceView(val tags: DuplicatedTags<Int>)
data class TargetView(val tags: List<TargetTag>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<Int, Int>): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "tags = source.tags.map { this.toTag(source = it) }")
    }

    @Test
    fun resolveTypealiasToNestedClassOfOtherPackageUsedViaImportAlias() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = arrayOf(
                SourceFile.kotlin(
                    name = "a/Person.kt",
                    contents =
                        """
package a

data class Person(val name: String, val address: Address) {
    data class Address(val street: String)
}
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "TestCode.kt",
                    contents =
                        """
import a.Person.Address as Addr

typealias SourceAddress = Addr
typealias SourceAddresses = List<SourceAddress>

data class SourceView(val addresses: SourceAddresses, val mainAddress: SourceAddress?)
data class TargetAddress(val street: String)
data class TargetView(val addresses: List<TargetAddress>, val mainAddress: TargetAddress?)
                    """.trimIndent()
                ),
                SourceFile.kotlin(
                    name = "Mapper.kt",
                    contents =
                        """
import io.mcarle.konvert.api.Konverter
import a.Person.Address as SourceAddr

@Konverter
interface Mapper {
    fun toAddress(source: SourceAddr): TargetAddress
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
                ),
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "addresses = source.addresses.map { this.toAddress(source = it) }")
        assertContains(mapperCode, "mainAddress = source.mainAddress?.let { this.toAddress(source = it) }")
    }

    @Test
    fun resolveRecursiveDataStructureDefinedViaTypealias() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceNode(val name: String, val children: SourceNodes)
typealias SourceNodes = Nodes<SourceNode>
typealias Nodes<T> = List<T>

data class TargetNode(val name: String, val children: List<TargetNode>)

@Konverter
interface Mapper {
    fun toNode(source: SourceNode): TargetNode
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "children = source.children.map { this.toNode(source = it) }")
    }

    @Test
    fun resolveTypealiasWithUnusedTypeParameter() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IntToStringConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag<T>(val id: T)
data class TargetTag(val id: String)

typealias IgnoringSecond<T, X> = SourceTag<T>
typealias Tags<T> = List<T>

data class SourceView(val tags: Tags<IgnoringSecond<Int, Boolean>>)
data class TargetView(val tags: List<TargetTag>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag<Int>): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "tags = source.tags.map { this.toTag(source = it) }")
    }

    @Test
    fun resolveTypealiasWithSwappedAndNullableMapTypeArguments() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), MapToMapConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag(val id: Int)
data class TargetTag(val id: Int)

typealias SwappedNullableMap<V, K> = Map<K, V?>
typealias TagsByName = SwappedNullableMap<SourceTag, String>

data class SourceView(val tags: TagsByName)
data class TargetView(val tags: Map<String, TargetTag?>)

@Konverter
interface Mapper {
    fun toTag(source: SourceTag): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "tags = source.tags.mapValues { (_, it) -> it?.let { this.toTag(source = it) } }")
    }

    @Test
    fun issue221() {
        val (compilation) = compileWith(
            enabledConverters = listOf(SameTypeConverter(), IterableToListConverter()),
            code = SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                    """
import io.mcarle.konvert.api.Konverter

data class SourceTag(val id: Int, val name: String)

typealias SourceTagsView = List<SourceTag>

data class SourceView(
    val id: Int,
    val tags: SourceTagsView,
)

data class TargetTag(val id: Int, val name: String)

typealias TargetTagsView = List<TargetTag>

data class TargetView(
    val id: Int,
    val tags: TargetTagsView,
)

@Konverter
interface BugReproMapper {
    fun toTag(source: SourceTag): TargetTag
    fun toView(source: SourceView): TargetView
}
                    """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("BugReproMapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "tags = source.tags.map { this.toTag(source = it) }")
    }
}





