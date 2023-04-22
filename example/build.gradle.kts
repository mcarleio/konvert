import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    id("com.google.devtools.ksp").version("1.8.10-1.0.9")
}

val konvertVersion = "0.1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.mcarle:konvert-api:$konvertVersion")
    implementation("io.mcarle:konvert-spring-annotations:$konvertVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    // KSP to generate mapping code
    ksp("io.mcarle:konvert:$konvertVersion")
    ksp("io.mcarle:konvert-spring-injector:$konvertVersion")
    // only needed if you need to enable specific converter through Mapping(enable=...)
    compileOnly("io.mcarle:konvert-converter:$konvertVersion")
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
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.javaParameters = true
}
