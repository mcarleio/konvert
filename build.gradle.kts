plugins {
    base
    id("org.jetbrains.kotlinx.kover") version "0.7.0-Alpha"
}

dependencies {
    kover(project(":converter"))
    kover(project(":processor"))
}

repositories {
    mavenCentral()
}

koverReport {
    filters {
        includes {
            packages("io.mcarle.kmap")
        }
    }
}