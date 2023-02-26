package io.mcarle.lib.kmapper.processor.converter

import com.tschuchort.compiletesting.KotlinCompilation
import io.mcarle.lib.kmapper.processor.TypeConverter
import org.jetbrains.kotlin.util.suffixIfNot
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.provider.Arguments
import java.io.File

fun <E : TypeConverter> List<E>.toConverterTestArguments(typeNameExtractor: (E) -> Pair<String?, String?>) = this.flatMap {
    val (sourceTypeName, targetTypeName) = typeNameExtractor(it)
    listOf(
        Arguments.arguments(Named.named(it::class.simpleName, it), sourceTypeName, targetTypeName),
        Arguments.arguments(Named.named(it::class.simpleName, it), sourceTypeName, "$targetTypeName?"),
        Arguments.arguments(Named.named(it::class.simpleName, it), "$sourceTypeName?", "$targetTypeName?")
    )
}

fun Iterable<List<String>>.removeSourceNullableAndTargetNotNull() = filterNot { it[0].endsWith("?") && !it[1].endsWith("?") }
fun Iterable<List<String>>.removeSourceEqualsTarget() = filter { it[0].suffixIfNot("?") != it[1].suffixIfNot("?") }

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