package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
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

@OptIn(ExperimentalCompilerApi::class)
val KotlinCompilation.kspWorkingDir: File
    get() = workingDir.resolve("ksp")

@OptIn(ExperimentalCompilerApi::class)
val KotlinCompilation.kspSourcesDir: File
    get() = kspWorkingDir.resolve("sources")
