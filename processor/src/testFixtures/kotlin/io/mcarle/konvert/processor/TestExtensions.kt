package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.KotlinCompilation
import java.io.File

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
