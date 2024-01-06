plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}

dependencies {
    api(project(":annotations"))
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("${layout.buildDirectory.get().asFile}/generated/generator/kotlin"))
        }
    }
}

kotlin {
    target {
        compilations {
            @Suppress("UNUSED_VARIABLE")
            val generator by compilations.creating {
                defaultSourceSet {
                    dependencies {
                        parent?.subprojects?.forEach {
                            if (it.path !in arrayOf(":injectors", ":docs", ":annotations", path)) {
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
                    args = listOf("${layout.buildDirectory.get().asFile}/generated/generator/kotlin")
                }

                tasks.processResources {
                    dependsOn += generateTask
                }
                tasks.compileKotlin {
                    dependsOn += generateTask
                }
                tasks.sourcesJar {
                    dependsOn += generateTask
                }
            }
        }
    }
}
