import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.google.devtools.ksp").version("2.1.20-1.0.31")
}

val konvertVersion = "0.1.0-SNAPSHOT"

val jUnitVersion = "5.12.1"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.mcarle:konvert-api:$konvertVersion")
    implementation("io.mcarle:konvert-spring-annotations:$konvertVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jUnitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // KSP to generate mapping code
    ksp("io.mcarle:konvert:$konvertVersion")
    ksp("io.mcarle:konvert-spring-injector:$konvertVersion")
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        javaParameters = true
    }
}

ksp {
    arg("konvert.konverter.generate-class", "true")
}
