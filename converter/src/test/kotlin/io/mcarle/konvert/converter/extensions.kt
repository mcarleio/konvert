package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.params.provider.Arguments
import org.paukov.combinatorics3.Generator

fun <T> List<T>.toConverterTestArguments(typeNameExtractor: (T) -> Pair<String?, String?>) = this.flatMap {
    val (sourceTypeName, targetTypeName) = typeNameExtractor(it)
    listOf(
        Arguments.arguments(sourceTypeName, targetTypeName),
        Arguments.arguments(sourceTypeName, "$targetTypeName?"),
        Arguments.arguments("$sourceTypeName?", "$targetTypeName?")
    )
}

fun <T> List<T>.cartesianProductWithNullableCombinations(vararg other: T) = Generator.cartesianProduct(this, other.toList())
    .flatMap {
        val sourceTypeName = it.first()
        val targetTypeName = it.last()
        listOf(
            Arguments.arguments(sourceTypeName, targetTypeName),
            Arguments.arguments(sourceTypeName, "$targetTypeName?"),
            Arguments.arguments("$sourceTypeName?", "$targetTypeName"),
            Arguments.arguments("$sourceTypeName?", "$targetTypeName?")
        )
    }

fun <E : TypeConverter> List<E>.toConverterTestArgumentsWithType(typeNameExtractor: (E) -> Pair<String?, String?>) = this.flatMap {
    val (sourceTypeName, targetTypeName) = typeNameExtractor(it)
    listOf(
        Arguments.arguments(it::class.simpleName, sourceTypeName, targetTypeName),
        Arguments.arguments(it::class.simpleName, sourceTypeName, "$targetTypeName?"),
        Arguments.arguments(it::class.simpleName, "$sourceTypeName?", "$targetTypeName?")
    )
}

fun <T : TypeConverter> Set<Class<out T>>.newConverterInstance(simpleConverterName: String): T =
    first { it.simpleName == simpleConverterName }
        .getDeclaredConstructor()
        .newInstance()

fun Iterable<List<String>>.removeSourceNullableAndTargetNotNull() = filterNot { it[0].endsWith("?") && !it[1].endsWith("?") }
