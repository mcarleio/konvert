import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") // version defined in buildSrc/build.gradle.kts -> dependencies
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    implementation(kotlinStdlib)
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kover {
    useKoverTool()
    disabledForProject = System.getenv("CI") == null
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.javaParameters = true
}
