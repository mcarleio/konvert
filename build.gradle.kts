plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}

dependencies {
    implementation(project(":converter"))
    implementation(project(":processor"))

    // add kover output from all non-empty modules
    subprojects.forEach {
        if (it.path !in arrayOf(":injectors", ":docs")) {
            kover(project(it.path))
        }
    }
}

kover {
    reports {
        filters {
            includes {
                packages("io.mcarle.konvert")
            }
        }
        total {
            html {
                onCheck = true
            }
            xml {
                onCheck = true
            }
        }
    }
}

/**
 * Include the generated META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider from :processor module
 *
 * This is a workaround for later use with maven, as atm during KSP only the JAR itself is searched
 */
val copySymbolProcessorProvider = tasks.register<Copy>("copySymbolProcessorProvider") {
    description = "Copies the generated META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider from :processor module"
    group = LifecycleBasePlugin.BUILD_GROUP

    dependsOn(configurations.runtimeClasspath)
    from({ zipTree(project(":processor").tasks.jar.get().archiveFile) }) {
        include("**/*.SymbolProcessorProvider")
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("classes/kotlin/main/"))
}

tasks.named<Copy>("processResources") {
    dependsOn(copySymbolProcessorProvider)
}

afterEvaluate {
    tasks.named("koverGenerateArtifactJvm") {
        dependsOn(copySymbolProcessorProvider)
    }
}
