package io.mcarle.konvert.testkit

import io.mcarle.konvert.api.config.ENABLE_CONVERTERS
import io.mcarle.konvert.api.config.KONVERTER_GENERATE_CLASS
import io.mcarle.konvert.api.converter.INT_TO_ENUM_CONVERTER
import io.mcarle.konvert.api.converter.LONG_EPOCH_MILLIS_TO_INSTANT_CONVERTER
import io.mcarle.konvert.api.converter.STRING_TO_DATE_CONVERTER
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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

        @JvmStatic
        fun commonTypeCombinations(): List<Arguments> = listOf(
            Arguments.of("Int", "Double", null),
            Arguments.of("List<Int>", "Set<String>", null),
            Arguments.of("Map<Int, Int>", "MutableMap<String, Long>", null),
            Arguments.of("DeprecationLevel", "String", null),
            Arguments.of("Int", "DeprecationLevel", INT_TO_ENUM_CONVERTER),
        )

        @JvmStatic
        fun jvmTypeCombinations(): List<Arguments> = commonTypeCombinations() + listOf(
            Arguments.of("java.util.Date", "java.time.Instant", null),
            Arguments.of("java.util.Date", "String", null),
            Arguments.of("java.time.Instant", "java.util.Date", null),
            Arguments.of("java.time.OffsetDateTime", "java.time.Instant", null),
            Arguments.of("java.time.Instant", "Long", null),
            Arguments.of("String", "java.util.Date", STRING_TO_DATE_CONVERTER),
            Arguments.of("Long", "java.time.Instant", LONG_EPOCH_MILLIS_TO_INSTANT_CONVERTER),
            Arguments.of("java.util.concurrent.TimeUnit", "kotlin.time.DurationUnit", null),
        )
    }

    // region: Helper methods

    private fun settingsGradleKts(projectName: String = "konvert-kmp-test") = """
            rootProject.name = "$projectName"
        """.trimIndent()

    private fun buildGradleKts(
        targets: String = """
                jvm()
                js {
                    browser()
                    nodejs()
                }
            """.trimIndent(),
        kspDependencies: String = """
                add("kspCommonMainMetadata", "io.mcarle:konvert:$konvertVersion")
                add("kspJvm", "io.mcarle:konvert:$konvertVersion")
                add("kspJs", "io.mcarle:konvert:$konvertVersion")
            """.trimIndent(),
        additionalDependencies: String = """
                implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
            """.trimIndent(),
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

    private fun findGeneratedFile(platform: String, vararg pathSegments: String): File? {
        return projectDir.resolve("build/generated/ksp")
            .resolve(platform)
            .walkTopDown()
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "SourceKonverter.kt")
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "PersonKonverter.kt")
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "TargetEventKonverter.kt")
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "UserMapperKonverter.kt")
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "AddressMapperKonverter.kt")
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

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "ColoredItemKonverter.kt")
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "NullableSourceKonverter.kt")
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

                        import io.mcarle.konvert.api.Konverter
                        import io.mcarle.konvert.api.KonvertTo

                        data class TagDto(val name: String)
                        data class MetadataKeyDto(val key: String)
                        data class MetadataValueDto(val value: String)

                        @Konverter
                        interface Mapper {
                            fun stringToTagDto(str: String): TagDto = TagDto(str)
                            fun stringToMetadataKeyDto(str: String): MetadataKeyDto = MetadataKeyDto(str)
                            fun stringToMetadataValueDto(str: String): MetadataValueDto = MetadataValueDto(str)
                        }

                        @KonvertTo(TagContainerDto::class)
                        data class TagContainer(val tags: List<String>, val metadata: Map<String, String>)

                        data class TagContainerDto(val tags: Set<TagDto>, val metadata: Map<MetadataKeyDto, MetadataValueDto>)
                    """.trimIndent()
                )
            )

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "TagContainerKonverter.kt")
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

            val result = runGradle("assemble")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            // Verify both common and JVM-specific files are generated
            val commonGenerated = findGeneratedFile("metadata", "CommonSourceKonverter.kt")
            val jvmGenerated = findGeneratedFile("jvm", "JvmSourceKonverter.kt")

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
                    "test/sub/JvmModels.kt" to """
                        package test.sub

                        import test.SharedEntity
                        import test.SharedDto
                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(JvmDto::class)
                        data class JvmEntity(val uuid: String, val payload: SharedEntity)

                        data class JvmDto(val uuid: String, val payload: SharedDto)
                    """.trimIndent()
                )
            )

            val result = runGradle("assemble")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            // Verify both common and JVM-specific files are generated
            val commonGenerated = findGeneratedFile("metadata", "SharedEntityKonverter.kt")
            val jvmGenerated = findGeneratedFile("jvm", "JvmEntityKonverter.kt")

            assertNotNull(commonGenerated, "Common generated file should exist. All: ${findAllGeneratedFiles()}")
            assertNotNull(jvmGenerated, "JVM generated file should exist. All: ${findAllGeneratedFiles()}")

            val jvmGeneratedCode = jvmGenerated.readText()
            assertContains(jvmGeneratedCode, "fun JvmEntity.toJvmDto()")
        }

        @Test
        fun `class from commonMain used by mapping in jvmMain`() {
            writeProjectFiles(
                commonMainFiles = mapOf(
                    "test/CommonModels.kt" to """
                        package test

                        data class Common(val id: Int, val name: String)
                    """.trimIndent()
                ),
                jvmMainFiles = mapOf(
                    "test/JvmModels.kt" to """
                        package test.sub

                        import test.Common
                        import io.mcarle.konvert.api.KonvertTo
                        import io.mcarle.konvert.api.KonvertFrom
                        import io.mcarle.konvert.api.Konfig
                        import io.mcarle.konvert.api.config.ENABLE_CONVERTERS
                        import io.mcarle.konvert.api.converter.STRING_TO_INT_CONVERTER

                        @KonvertFrom(Common::class)
                        @KonvertTo(Common::class, options = [
                            Konfig(key = ENABLE_CONVERTERS, value = STRING_TO_INT_CONVERTER)
                        ])
                        data class Jvm(val id: String, val name: String){
                            companion object {}
                        }

                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val jvmGenerated = findGeneratedFile("jvm", "JvmKonverter.kt")

            assertNotNull(jvmGenerated, "JVM generated file should exist. All: ${findAllGeneratedFiles()}")

            val jvmGeneratedCode = jvmGenerated.readText()
            assertContains(jvmGeneratedCode, "fun Jvm.toCommon()")
            assertContains(jvmGeneratedCode, "fun Jvm.Companion.fromCommon(common: Common)")
        }
    }

    // endregion

    // region: Tests for JS target compilation

    @Nested
    inner class KotlinMultiplatformJSTest {
        @Test
        fun `KSP generates code for JS target compilation`() {
            writeProjectFiles(
                jsMainFiles = mapOf(
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

            val generatedFile = findGeneratedFile("js", "MessageKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")
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
                            arg("$KONVERTER_GENERATE_CLASS", "true")
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "SimpleMapperKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            // With generate-class=true, it should generate a class instead of object
            assertContains(generatedCode, "class SimpleMapperImpl")
        }

        @Test
        fun `platform specific KSP options take priority over global KMP options`() {
            writeProjectFiles(
                buildScript = buildGradleKts(
                    kspOptions = """
                        ksp {
                            arg("$KONVERTER_GENERATE_CLASS", "true")
                        }

                        tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
                            if (name == "kspKotlinJvm") {
                                commandLineArgumentProviders.add(
                                    CommandLineArgumentProvider { listOf("$KONVERTER_GENERATE_CLASS=false") }
                                )
                            }
                        }

                    """.trimIndent()
                ),
                jvmMainFiles = mapOf(
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
                ),
                jsMainFiles = mapOf(
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

            val result = runGradle("assemble")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJs")?.outcome)

            val generatedJvmFile = findGeneratedFile("jvm", "SimpleMapperKonverter.kt")
            val generatedJsFile = findGeneratedFile("js", "SimpleMapperKonverter.kt")
            assertNotNull(
                generatedJvmFile,
                "Generated konverter file should exist for JVM. All generated files: ${findAllGeneratedFiles()}"
            )
            assertNotNull(generatedJsFile, "Generated konverter file should exist for JS. All generated files: ${findAllGeneratedFiles()}")

            val generatedJvmCode = generatedJvmFile.readText()
            // With generate-class=false, it should generate an object instead of class
            assertContains(generatedJvmCode, "object SimpleMapperImpl")

            val generatedJsCode = generatedJsFile.readText()
            // With generate-class=true, it should generate a class instead of object
            assertContains(generatedJsCode, "class SimpleMapperImpl")
        }
    }

    // endregion


    // region: Tests for @Mapping args in KMP context

    @Nested
    inner class KotlinMultiplatformMappingArgsTest {

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
                            val amount: Long,
                            val active: Boolean
                        )
                    """.trimIndent()
                )
            )

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "OrderKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun Order.toOrderDto()")
            assertContains(generatedCode, "amount = quantity.toLong()")
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "ConfigKonverter.kt")
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

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "NamePartsKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")

            val generatedCode = generatedFile.readText()
            assertContains(generatedCode, "fun NameParts.toDisplayName()")
            assertContains(generatedCode, """it.first + " " + it.last""")
        }
    }

    // endregion

    // region: Tests for type converters in KMP context

    @Nested
    inner class KotlinMultiplatformTypeConverterTest {

        @ParameterizedTest
        @MethodSource("io.mcarle.konvert.testkit.KotlinMultiplatformGradleTestkitITest#commonTypeCombinations")
        fun `KSP handles type converters in commonMain`(sourceType: String, targetType: String, enabledConverter: String? = null) {
            writeProjectFiles(
                buildGradleKts(
                    kspOptions = """
                    ksp {
                        arg("$ENABLE_CONVERTERS", "$enabledConverter")
                    }
                """.trimIndent()
                ),
                commonMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(Target::class)
                        data class Source(
                            val value: $sourceType,
                            val valueOpt: $sourceType?,
                        )

                        data class Target(
                            val value: $targetType,
                            val valueOpt: $targetType?,
                        )
                    """.trimIndent()
                )
            )

            val result = runGradle("compileCommonMainKotlinMetadata")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileCommonMainKotlinMetadata")?.outcome)

            val generatedFile = findGeneratedFile("metadata", "SourceKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")
        }

        @ParameterizedTest
        @MethodSource("io.mcarle.konvert.testkit.KotlinMultiplatformGradleTestkitITest#jvmTypeCombinations")
        fun `KSP handles type converters in jvmMain`(sourceType: String, targetType: String, enabledConverter: String? = null) {
            writeProjectFiles(
                buildGradleKts(
                    kspOptions = """
                    ksp {
                        arg("$ENABLE_CONVERTERS", "$enabledConverter")
                    }
                """.trimIndent()
                ),
                jvmMainFiles = mapOf(
                    "test/Models.kt" to """
                        package test

                        import io.mcarle.konvert.api.KonvertTo

                        @KonvertTo(Target::class)
                        data class Source(
                            val value: $sourceType,
                            val valueOpt: $sourceType?,
                        )

                        data class Target(
                            val value: $targetType,
                            val valueOpt: $targetType?,
                        )
                    """.trimIndent()
                )
            )

            val result = runGradle("compileKotlinJvm")

            assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)

            val generatedFile = findGeneratedFile("jvm", "SourceKonverter.kt")
            assertNotNull(generatedFile, "Generated konverter file should exist. All generated files: ${findAllGeneratedFiles()}")
        }

    }

    // endregion
}









