plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}

dependencies {
    implementation(project(":converter"))
    implementation(project(":processor"))

    kover(project(":api"))
    kover(project(":converter"))
    kover(project(":converter-api"))
    kover(project(":injectors:cdi-annotations"))
    kover(project(":injectors:cdi-injector"))
    kover(project(":injectors:spring-annotations"))
    kover(project(":injectors:spring-injector"))
    kover(project(":injectors:koin-annotations"))
    kover(project(":injectors:koin-injector"))
    kover(project(":plugin-api"))
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
    disabledForProject = true
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
