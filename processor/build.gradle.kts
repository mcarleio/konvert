import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    kotlin("jvm") version "1.7.22"
    id("com.google.devtools.ksp").version("1.7.22-1.0.8")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":annotation"))
    implementation("org.jetbrains:annotations:23.1.0")

    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.22-1.0.8")
    implementation("com.google.devtools.ksp:symbol-processing:1.7.22-1.0.8")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.7.22")

    implementation("com.squareup:kotlinpoet:1.12.0")
    implementation("com.squareup:kotlinpoet-ksp:1.12.0")


    implementation("com.github.dpaukov:combinatoricslib3:3.3.3")


    // auto service
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.0.0")


    testImplementation(project(":annotation"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("reflect"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testImplementation("org.reflections:reflections:0.10.2")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.9")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.9")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview"
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1.coerceAtLeast(Runtime.getRuntime().availableProcessors() / 2)
}

ksp {
    arg("autoserviceKsp.verify", "true")
    arg("autoserviceKsp.verbose", "true")
}