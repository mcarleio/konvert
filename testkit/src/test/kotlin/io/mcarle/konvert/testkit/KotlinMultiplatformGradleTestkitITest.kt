package io.mcarle.konvert.testkit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration tests for Konvert KSP processor in Kotlin Multiplatform projects.
 *
 * These tests use Gradle TestKit to run real KMP builds with KSP, verifying that
 * the processor works correctly for common, JVM, and (here) JS compilations.
 *
 * Coverage data from the KSP processor execution can be collected via Kover's
 * JVM agent and will then be used as the coverage report from this module.
 */
class KotlinMultiplatformGradleTestkitITest {

    @TempDir
    lateinit var projectDir: File

    private val kotlinVersion = System.getProperty("testkitTests.kotlin.version") ?: "2.3.21"
    private val kspVersion = System.getProperty("testkitTests.ksp.version") ?: "2.3.9"
    private val konvertVersion = System.getProperty("testkitTests.konvert.version") ?: "0.1.0-SNAPSHOT"

    companion object {
        private val koverAgentJar: String? = System.getProperty("testkitTests.kover.agent.jar")
        private val koverReportDir: String? = System.getProperty("testkitTests.kover.report.dir")
        private val mavenTestRepoDir: String? = System.getProperty("testkitTests.maven.test.repo.dir")

        private val koverAgentArgsFile: File? by lazy {
            if (koverAgentJar == null || koverReportDir == null) return@lazy null
            val reportDir = File(koverReportDir).apply { mkdirs() }
            val reportFile = reportDir.resolve("kmp.ic") // same file for all tests, allows reusing Gradle daemon
            reportDir.resolve("kover-agent.args").apply {
                writeText(
                    buildString {
                        appendLine("report.file=${reportFile.absolutePath}")
                        appendLine("report.append=true")
                        appendLine("include=io.mcarle.konvert.*")
                    }
                )
            }
        }

        private val gradleJvmArgs: String by lazy {
            buildString {
                append("-Xmx2g -XX:MaxMetaspaceSize=512m") // increase defaults to prevent flaky tests
                koverAgentArgsFile?.let { argsFile ->
                    append(" -javaagent:$koverAgentJar=file:${argsFile.absolutePath}")
                }
            }
        }
    }

    // region: Helper methods

    private fun settingsGradleKts(projectName: String = "konvert-kmp-test") = """
            rootProject.name = "$projectName"
        """.trimIndent()

    private fun buildGradleKts(
        targets: String = """
                jvm()
            """.trimIndent(),
        kspDependencies: String = """
                add("kspCommonMainMetadata", "io.mcarle:konvert:$konvertVersion")
                add("kspJvm", "io.mcarle:konvert:$konvertVersion")
            """.trimIndent(),
        additionalDependencies: String = "",
        kspOptions: String = ""
    ) = """
            import org.jetbrains.kotlin.gradle.dsl.JvmTarget

            plugins {
                kotlin("multiplatform") version "$kotlinVersion"
                id("com.google.devtools.ksp") version "$kspVersion"
            }

            repositories {
                ${if (mavenTestRepoDir != null) """maven { name = "testkit"; url = uri("$mavenTestRepoDir") }""" else "mavenLocal()"}
                mavenCentral()
            }

            kotlin {
                $targets

                sourceSets {
                    commonMain {
                        dependencies {
                            implementation("io.mcarle:konvert-api:$konvertVersion")
                            $additionalDependencies
                        }
                    }
                }
            }

            dependencies {
                $kspDependencies
            }

            $kspOptions
        """.trimIndent()

    private fun buildGradleKtsWithJvmAndJs(
        kspDependencies: String = """
                add("kspCommonMainMetadata", "io.mcarle:konvert:$konvertVersion")
                add("kspJvm", "io.mcarle:konvert:$konvertVersion")
                add("kspJs", "io.mcarle:konvert:$konvertVersion")
            """.trimIndent()
    ) = buildGradleKts(
        targets = """
                jvm()
                js {
                    browser()
                    nodejs()
                }
            """.trimIndent(),
        kspDependencies = kspDependencies,
        additionalDependencies = """
                implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
            """.trimIndent()
    )

    private fun writeProjectFiles(
        buildScript: String = buildGradleKts(),
        commonMainFiles: Map<String, String> = emptyMap(),
        jvmMainFiles: Map<String, String> = emptyMap(),
        jsMainFiles: Map<String, String> = emptyMap()
    ) {
        projectDir.resolve("settings.gradle.kts").writeText(settingsGradleKts())
        projectDir.resolve("build.gradle.kts").writeText(buildScript)

        projectDir.resolve("gradle.properties").writeText(
            """
                org.gradle.jvmargs=$gradleJvmArgs
                org.gradle.daemon=true
            """.trimIndent()
        )

        commonMainFiles.forEach { (path, content) ->
            val file = projectDir.resolve("src/commonMain/kotlin/$path")
            file.parentFile.mkdirs()
            file.writeText(content)
        }
        jvmMainFiles.forEach { (path, content) ->
            val file = projectDir.resolve("src/jvmMain/kotlin/$path")
            file.parentFile.mkdirs()
            file.writeText(content)
        }
        jsMainFiles.forEach { (path, content) ->
            val file = projectDir.resolve("src/jsMain/kotlin/$path")
            file.parentFile.mkdirs()
            file.writeText(content)
        }
    }

    private fun runGradle(vararg args: String): BuildResult {
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*args, "--stacktrace")
            .forwardOutput()
            .build()
    }

    private fun findGeneratedFile(vararg pathSegments: String): File? {
        return projectDir.resolve("build").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .firstOrNull { file ->
                pathSegments.all { segment -> file.path.contains(segment) }
            }
    }

    private fun findAllGeneratedFiles(): List<File> {
        return projectDir.resolve("build").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.path.contains("ksp") }
            .toList()
    }

    // endregion

    // region: Tests for @KonvertTo in commonMain

    @Nested
    inner class KonvertToTest {

        @Test
        fun `KonvertTo in commonMain generates extension function`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(TargetDto::class)
                        data class Source(val name: String, val age: Int)

                        data class TargetDto(val name: String, val age: Int)
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("SourceKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun Source.toTargetDto()")
            assertContains(generatedCode, "name = name")
            assertContains(generatedCode, "age = age")
        }

        @Test
        fun `KonvertTo with property mapping in commonMain`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo
                        import io.mcarle.konvert.api.Mapping

                        @KonvertTo(
                            PersonDto::class,
                            mappings = [
                                Mapping(source = "firstName", target = "name"),
                                Mapping(source = "yearsOld", target = "age")
                            ]
                        )
                        data class Person(val firstName: String, val yearsOld: Int)

                        data class PersonDto(val name: String, val age: Int)
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("PersonKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun Person.toPersonDto()")
            assertContains(generatedCode, "name = firstName")
            assertContains(generatedCode, "age = yearsOld")
        }
    }

    // endregion

    // region: Tests for @KonvertFrom in commonMain

    @Nested
    inner class KonvertFromTest {

        @Test
        fun `KonvertFrom in commonMain generates extension function`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertFrom

                        data class SourceEvent(val id: Long, val description: String)

                        data class TargetEvent(val id: Long, val description: String) {
                            @KonvertFrom(SourceEvent::class)
                            companion object
                        }
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("TargetEventKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun TargetEvent.Companion.fromSourceEvent")
            assertContains(generatedCode, "id = sourceEvent.id")
            assertContains(generatedCode, "description = sourceEvent.description")
        }

    }

    // endregion

    // region: Tests for @Konverter interface in commonMain

    @Nested
    inner class KonverterTest {

        @Test
        fun `Konverter interface in commonMain generates implementation`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.Konverter

                        data class DomainUser(val name: String, val email: String)
                        data class UserDto(val name: String, val email: String)

                        @Konverter
                        interface UserMapper {
                            fun toDto(user: DomainUser): UserDto
                        }
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("UserMapperKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "object UserMapperImpl")
            assertContains(generatedCode, "override fun toDto(user: DomainUser): UserDto")
            assertContains(generatedCode, "name = user.name")
            assertContains(generatedCode, "email = user.email")
        }

        @Test
        fun `Konverter interface with multiple functions in commonMain`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.Konverter
                        import io.mcarle.konvert.api.Konvert
                        import io.mcarle.konvert.api.Mapping

                        data class Address(val street: String, val city: String, val zip: String)
                        data class AddressDto(val streetName: String, val cityName: String, val zipCode: String)

                        @Konverter
                        interface AddressMapper {
                            @Konvert(mappings = [
                                Mapping(source = "street", target = "streetName"),
                                Mapping(source = "city", target = "cityName"),
                                Mapping(source = "zip", target = "zipCode")
                            ])
                            fun toDto(address: Address): AddressDto

                            @Konvert(mappings = [
                                Mapping(source = "streetName", target = "street"),
                                Mapping(source = "cityName", target = "city"),
                                Mapping(source = "zipCode", target = "zip")
                            ])
                            fun toDomain(dto: AddressDto): Address
                        }
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("AddressMapperKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "object AddressMapperImpl")
            assertContains(generatedCode, "override fun toDto(address: Address): AddressDto")
            assertContains(generatedCode, "override fun toDomain(dto: AddressDto): Address")
            assertContains(generatedCode, "streetName = address.street")
            assertContains(generatedCode, "street = dto.streetName")
        }
    }

    // endregion

    // region: Tests for KMP multi-target builds

    @Nested
    inner class KotlinMultiplatformBasicTest {

        @Test
        fun `KMP project with JVM and JS targets compiles successfully`() {
            writeProjectFiles(
                buildScript = buildGradleKtsWithJvmAndJs(),
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(ItemDto::class)
                        data class Item(val id: Int, val label: String)

                        data class ItemDto(val id: Int, val label: String)
                    """.trimIndent()
                )
            )

            val result = runGradle("assemble")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJs")?.outcome)
        }

        @Test
        fun `KSP processes enum mapping in commonMain`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        enum class SourceColor { RED, GREEN, BLUE }
                        enum class TargetColor { RED, GREEN, BLUE }

                        @KonvertTo(ColorDto::class)
                        data class ColoredItem(val name: String, val color: SourceColor)

                        data class ColorDto(val name: String, val color: TargetColor)
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("ColoredItemKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun ColoredItem.toColorDto()")
        }

        @Test
        fun `KSP processes nullable types in commonMain`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(NullableTarget::class)
                        data class NullableSource(
                            val required: String,
                            val optional: String?
                        )

                        data class NullableTarget(
                            val required: String,
                            val optional: String?
                        )
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("NullableSourceKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun NullableSource.toNullableTarget()")
            assertContains(generatedCode, "required = required")
            assertContains(generatedCode, "optional = optional")
        }

        @Test
        fun `KSP processes collection types in commonMain`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(TagContainerDto::class)
                        data class TagContainer(val tags: List<String>, val metadata: Map<String, String>)

                        data class TagContainerDto(val tags: List<String>, val metadata: Map<String, String>)
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("TagContainerKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun TagContainer.toTagContainerDto()")
        }
    }


    // endregion

    // region: Tests for platform-specific KSP runs

    @Nested
    inner class KotlinMultiplatformPlatformSpecificTest {

        @Test
        fun `KSP processes platform-specific code in jvmMain separately`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/CommonModels.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(CommonTarget::class)
                        data class CommonSource(val id: String, val value: Int)

                        data class CommonTarget(val id: String, val value: Int)
                    """.trimIndent()
                ),
                jvmMainFiles = mapOf(
                    "test/JvmModels.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(JvmTarget::class)
                        data class JvmSource(val uuid: String, val payload: String)

                        data class JvmTarget(val uuid: String, val payload: String)
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            // Verify both common and JVM-specific files are generated
            val commonGenerated = findGeneratedFile("CommonSourceKonverter.kt")
            val jvmGenerated = findGeneratedFile("JvmSourceKonverter.kt")

            assertNotNull(commonGenerated, "Common generated file should exist. All: ${findAllGeneratedFiles()}")
            assertNotNull(jvmGenerated, "JVM generated file should exist. All: ${findAllGeneratedFiles()}")
        }

        @Test
        fun `Konverter in commonMain reused by jvmMain mapping`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/CommonModels.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(SharedDto::class)
                        data class SharedEntity(val id: String, val name: String)

                        data class SharedDto(val id: String, val name: String)
                    """.trimIndent()
                ),
                jvmMainFiles = mapOf(
                    "test/JvmModels.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(JvmDto::class)
                        data class JvmEntity(val uuid: String, val payload: String)

                        data class JvmDto(val uuid: String, val payload: String)
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            // Verify both common and JVM-specific files are generated
            val commonGenerated = findGeneratedFile("SharedEntityKonverter.kt")
            val jvmGenerated = findGeneratedFile("JvmEntityKonverter.kt")

            assertNotNull(commonGenerated, "Common generated file should exist. All: ${findAllGeneratedFiles()}")
            assertNotNull(jvmGenerated, "JVM generated file should exist. All: ${findAllGeneratedFiles()}")
        }
    }

    // endregion

    // region: Tests for type converters in KMP context

    @Nested
    inner class KotlinMultiplatformTypeConverterTest {

        @Test
        fun `KSP handles type conversion with mapping in commonMain`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo
                        import io.mcarle.konvert.api.Mapping

                        @KonvertTo(OrderDto::class, mappings = [
                            Mapping(source = "quantity", target = "amount")
                        ])
                        data class Order(
                            val id: String,
                            val quantity: Int,
                            val active: Boolean
                        )

                        data class OrderDto(
                            val id: String,
                            val amount: Int,
                            val active: Boolean
                        )
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("OrderKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun Order.toOrderDto()")
            assertContains(generatedCode, "amount = quantity")
            assertContains(generatedCode, "active = active")
        }

        @Test
        fun `KSP handles constant mapping in commonMain`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo
                        import io.mcarle.konvert.api.Mapping

                        @KonvertTo(ConfigDto::class, mappings = [
                            Mapping(target = "version", constant = "1")
                        ])
                        data class Config(val name: String, val enabled: Boolean)

                        data class ConfigDto(
                            val name: String,
                            val enabled: Boolean,
                            val version: Int = 0
                        )
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("ConfigKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun Config.toConfigDto()")
            assertContains(generatedCode, "version = 1")
        }

        @Test
        fun `KSP handles expression mapping in commonMain`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo
                        import io.mcarle.konvert.api.Mapping

                        @KonvertTo(DisplayName::class, mappings = [
                            Mapping(target = "fullName", expression = "it.first + \" \" + it.last")
                        ])
                        data class NameParts(val first: String, val last: String)

                        data class DisplayName(val fullName: String)
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("NamePartsKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun NameParts.toDisplayName()")
            assertContains(generatedCode, """it.first + " " + it.last""")
        }
    }

    // endregion

    // region: Tests for JS target compilation

    @Nested
    inner class KotlinMultiplatformJSTest {
        @Test
        fun `KSP generates code for JS target compilation`() {
            writeProjectFiles(
                buildScript = buildGradleKtsWithJvmAndJs(),
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(MessageDto::class)
                        data class Message(val id: String, val text: String, val timestamp: Long)

                        data class MessageDto(val id: String, val text: String, val timestamp: Long)
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJs")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJs")?.outcome)
        }
    }

    // endregion

    // region: KSP options in KMP context

    @Nested
    inner class KotlinMultiplatformKspOptionTest {

        @Test
        fun `KSP options are passed correctly in KMP build`() {
            writeProjectFiles(
                buildScript = buildGradleKts(
                    kspOptions = """
                        ksp {
                            arg("konvert.konverter.generate-class", "true")
                        }
                    """.trimIndent()
                ),
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.Konverter

                        data class Input(val value: String)
                        data class Output(val value: String)

                        @Konverter
                        interface SimpleMapper {
                            fun map(input: Input): Output
                        }
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("SimpleMapperKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            // With generate-class=true, it should generate a class instead of object
            assertContains(generatedCode, "class SimpleMapperImpl")
        }
    }

    // endregion
}









