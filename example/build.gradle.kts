plugins {
    kotlin("jvm") version "1.7.22"
    id("java-library")
    id("com.google.devtools.ksp").version("1.7.22-1.0.8")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":api"))
    implementation(project(":converter-api"))

    // only needed in case we enable specific converter
    compileOnly(project(":converter"))

    // KSP to generate mapping code
    ksp(project(":processor"))
    ksp(project(":converter"))
}

repositories {
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