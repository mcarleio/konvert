package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.KotlinCompilation
import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.params.provider.Arguments
import java.io.File

fun <T> List<T>.toConverterTestArguments(typeNameExtractor: (T) -> Pair<String?, String?>) = this.flatMap {
    val (sourceTypeName, targetTypeName) = typeNameExtractor(it)
    listOf(
        Arguments.arguments(sourceTypeName, targetTypeName),
        Arguments.arguments(sourceTypeName, "$targetTypeName?"),
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

fun KotlinCompilation.generatedSourceFor(fileName: String): String {
    return kspSourcesDir.walkTopDown()
        .firstOrNull { it.name == fileName }
        ?.readText()
        ?: throw IllegalArgumentException(
            "Unable to find $fileName in ${
                kspSourcesDir.walkTopDown().filter { it.isFile }.toList()
            }"
        )
}

val KotlinCompilation.kspWorkingDir: File
    get() = workingDir.resolve("ksp")

val KotlinCompilation.kspSourcesDir: File
    get() = kspWorkingDir.resolve("sources")
