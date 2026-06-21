import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform") version "2.3.21"
    id("com.google.devtools.ksp").version("2.3.9")
}

val konvertVersion = "0.1.0-SNAPSHOT"

val jUnitVersion = "6.1.0"


repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = JvmTarget.JVM_17
                    javaParameters = true
                }
            }
        }
    }
    js {
        browser()
        nodejs()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("io.mcarle:konvert-api:$konvertVersion")
            }
        }
        jvmMain {
            dependencies {
                implementation("io.mcarle:konvert-spring-annotations:$konvertVersion")
            }
        }
        jvmTest {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:$jUnitVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$jUnitVersion")
                runtimeOnly("org.junit.platform:junit-platform-launcher:$jUnitVersion")
            }
        }
    }
}

tasks.withType<KotlinJvmTest>().configureEach {
    useJUnitPlatform()
}


dependencies {
    add("kspCommonMainMetadata", "io.mcarle:konvert:$konvertVersion")
    add("kspJvm", "io.mcarle:konvert:$konvertVersion")
    add("kspJvm", "io.mcarle:konvert-spring-annotations:$konvertVersion")
}

ksp {
    arg("konvert.konverter.generate-class", "true")
}
