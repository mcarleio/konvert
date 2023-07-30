/**
 *
 * This is mostly a copy of
 * https://github.com/tschuchortdev/kotlin-compile-testing/blob/82ec5425c52e968ae706af0f667d8fe2fe5efdca/ksp/src/main/kotlin/com/tschuchort/compiletesting/Ksp.kt#L146
 *
 * The only relevant change to the KspCompileTestingComponentRegistrar class is:
 *
 * ```kotlin
 * this.languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_9, ApiVersion.KOTLIN_1_9)
 * ```
 *
 * This is needed to be able to run the KonverterITest's with Kotlin 1.9.0, as the library kotlin-compile-testing in version 1.5.0
 * does not support the kotlin 1.9.0 compiler. Once it does, this is no longer needed and can be removed!
 *
 */




package com.tschuchort.compiletesting

import com.google.devtools.ksp.AbstractKotlinSymbolProcessingExtension
import com.google.devtools.ksp.KspOptions
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.processing.impl.MessageCollectorBasedKSPLogger
import io.mcarle.konvert.processor.kspWorkingDir
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.com.intellij.core.CoreApplicationEnvironment
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.psi.PsiTreeChangeAdapter
import org.jetbrains.kotlin.com.intellij.psi.PsiTreeChangeListener
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
private class KspCompileTestingComponentRegistrar(
    private val compilation: KotlinCompilation
) : ComponentRegistrar {
    var providers = emptyList<SymbolProcessorProvider>()

    var options: MutableMap<String, String> = mutableMapOf()

    var incremental: Boolean = false
    var incrementalLog: Boolean = false
    var allWarningsAsErrors: Boolean = false
    var withCompilation: Boolean = false

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        if (providers.isEmpty()) {
            return
        }
        val options = KspOptions.Builder().apply {
            this.projectBaseDir = compilation.kspWorkingDir

            this.processingOptions.putAll(compilation.kspArgs)

            this.incremental = this@KspCompileTestingComponentRegistrar.incremental
            this.incrementalLog = this@KspCompileTestingComponentRegistrar.incrementalLog
            this.allWarningsAsErrors = this@KspCompileTestingComponentRegistrar.allWarningsAsErrors
            this.withCompilation = this@KspCompileTestingComponentRegistrar.withCompilation

            this.languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT

            this.cachesDir = compilation.kspCachesDir.also {
                it.deleteRecursively()
                it.mkdirs()
            }
            this.kspOutputDir = compilation.kspSourcesDir.also {
                it.deleteRecursively()
                it.mkdirs()
            }
            this.classOutputDir = compilation.kspClassesDir.also {
                it.deleteRecursively()
                it.mkdirs()
            }
            this.javaOutputDir = compilation.kspJavaSourceDir.also {
                it.deleteRecursively()
                it.mkdirs()
            }
            this.kotlinOutputDir = compilation.kspKotlinSourceDir.also {
                it.deleteRecursively()
                it.mkdirs()
            }
            this.resourceOutputDir = compilation.kspResources.also {
                it.deleteRecursively()
                it.mkdirs()
            }
            configuration[CLIConfigurationKeys.CONTENT_ROOTS]
                ?.filterIsInstance<JavaSourceRoot>()
                ?.forEach {
                    this.javaSourceRoots.add(it.file)
                }

        }.build()

        // Temporary until friend-paths is fully supported https://youtrack.jetbrains.com/issue/KT-34102
        @Suppress("invisible_member")
        val messageCollector = PrintingMessageCollector(
            compilation.internalMessageStreamAccess,
            MessageRenderer.GRADLE_STYLE,
            compilation.verbose
        )
        val messageCollectorBasedKSPLogger = MessageCollectorBasedKSPLogger(
            messageCollector = messageCollector,
            wrappedMessageCollector = messageCollector,
            allWarningsAsErrors = allWarningsAsErrors
        )
        val registrar = KspTestExtension(options, providers, messageCollectorBasedKSPLogger)
        AnalysisHandlerExtension.registerExtension(project, registrar)
        // Dummy extension point; Required by dropPsiCaches().
        CoreApplicationEnvironment.registerExtensionPoint(project.extensionArea, PsiTreeChangeListener.EP.name, PsiTreeChangeAdapter::class.java)
    }
}

private val KotlinCompilation.kspClassesDir: File
    get() = kspWorkingDir.resolve("classes")

private val KotlinCompilation.kspCachesDir: File
    get() = kspWorkingDir.resolve("caches")

private val KotlinCompilation.kspJavaSourceDir: File
    get() = kspSourcesDir.resolve("java")

private val KotlinCompilation.kspKotlinSourceDir: File
    get() = kspSourcesDir.resolve("kotlin")

private val KotlinCompilation.kspResources: File
    get() = kspSourcesDir.resolve("resources")

private class KspTestExtension(
    options: KspOptions,
    processorProviders: List<SymbolProcessorProvider>,
    logger: KSPLogger
) : AbstractKotlinSymbolProcessingExtension(
    options = options,
    logger = logger,
    testMode = false
) {
    private val loadedProviders = processorProviders

    override fun loadProviders() = loadedProviders
}
