plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
    id("org.jetbrains.kotlinx.kover") version "0.7.0-Alpha"
}

dependencies {
    implementation(project(":converter"))
    implementation(project(":processor"))
    kover(project(":converter"))
    kover(project(":processor"))
}

koverReport {
    filters {
        includes {
            packages("io.mcarle.konvert")
        }
    }
    html {
        onCheck = true
    }
    xml {
        onCheck = true
    }
}

kover {
    useKoverTool()
    disabledForProject = System.getenv("CI") == null
}

/**
 * Include the generated META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider from :processor module
 *
 * This is a workaround for later use with maven, as atm during KSP only the JAR itself is searched
 */
val copySymbolProcessorProvider = tasks.register<Copy>("copySymbolProcessorProvider") {
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
