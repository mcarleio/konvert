plugins {
    id("konvert.kotlin.kmp")
    id("konvert.mvn-publish.kmp")
}

val generatedDir = layout.buildDirectory.dir("generated/generator/kotlin")

kotlin {
    jvm {
        compilations {
            @Suppress("UNUSED_VARIABLE")
            val generator by compilations.creating {
                defaultSourceSet {
                    // Map to the existing source directory layout
                    kotlin.srcDir("src/generator/kotlin")

                    dependencies {
                        rootProject.subprojects.forEach {
                            if (it.path !in arrayOf(":injectors", ":docs", ":annotations", project.path)) {
                                implementation(project(it.path))
                            }
                        }
                        implementation("org.reflections:reflections:${Versions.orgReflections}")
                        implementation(kotlin("reflect"))
                    }
                }

                val generateTask = tasks.register<JavaExec>("generateApiConstants") {
                    description = "Generates constants for configurations and converters"
                    group = LifecycleBasePlugin.BUILD_GROUP
                    classpath = runtimeDependencyFiles + output.allOutputs
                    mainClass.set("GenerateKt")
                    args = listOf(generatedDir.get().asFile.absolutePath)
                    outputs.dir(generatedDir)
                }

                tasks.named("jvmProcessResources") {
                    dependsOn(generateTask)
                }
            }
        }
    }

    sourceSets {
        commonMain {
            // Use srcDir with the task provider so that KGP automatically
            // adds a task dependency from all compile tasks to generateApiConstants
            kotlin.setSrcDirs(emptyList<String>())
            kotlin.srcDir(tasks.named("generateApiConstants"))
            dependencies {
                api(project(":annotations"))
            }
        }
    }
}
