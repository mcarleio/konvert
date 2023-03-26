import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.kotlin

val DependencyHandler.kotlinTest get() = kotlin("test")
val DependencyHandler.kotlinReflect get() = kotlin("reflect")
val DependencyHandler.kotlinStdlib get() = kotlin("stdlib-jdk8")
val DependencyHandler.kotlinCompilerEmbeddable get() = kotlin("compiler-embeddable")

val DependencyHandler.kotlinCompileTesting get() = "com.github.tschuchortdev:kotlin-compile-testing:${Versions.kotlinCompileTesting}"
val DependencyHandler.kotlinCompileTestingKsp get() = "com.github.tschuchortdev:kotlin-compile-testing-ksp:${Versions.kotlinCompileTesting}"
val DependencyHandler.symbolProcessing get() = "com.google.devtools.ksp:symbol-processing:${Versions.kotlin}-${Versions.ksp}"
val DependencyHandler.symbolProcessingApi get() = "com.google.devtools.ksp:symbol-processing-api:${Versions.kotlin}-${Versions.ksp}"

val DependencyHandler.kotlinPoet get() = "com.squareup:kotlinpoet:${Versions.kotlinPoet}"
val DependencyHandler.kotlinPoetKsp get() = "com.squareup:kotlinpoet-ksp:${Versions.kotlinPoet}"

object Versions {
    const val kotlin = "1.8.10" // has to match buildSrc/gradle.properties
    const val ksp = "1.0.9"
    const val kotlinCompileTesting = "1.5.0"
    const val kotlinPoet = "1.12.0"
    const val jUnit = "5.9.2"
}