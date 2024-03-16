import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") // version defined in buildSrc/build.gradle.kts -> dependencies
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    implementation(kotlinStdlib)
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kover {
    if (System.getenv("CI") == null) {
        disable()
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.javaParameters = true
}
