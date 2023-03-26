import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    id("com.google.devtools.ksp").version("1.8.10-1.0.9")
}

val kmapVersion = "0.1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.mcarle:kmap-api:$kmapVersion")

    // KSP to generate mapping code
    ksp("io.mcarle:kmap-processor:$kmapVersion")
    ksp("io.mcarle:kmap-converter:$kmapVersion")
    // only needed if you need to enable specific converter through KMap(enable=...)
    compileOnly("io.mcarle:kmap-converter:$kmapVersion")
}

repositories {
    mavenLocal()
    mavenCentral()
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
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.javaParameters = true
}
