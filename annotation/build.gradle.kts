plugins {
    kotlin("jvm") version "1.7.22"
    id("java-library")
    id("maven-publish")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains:annotations:23.1.0")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.mcarle.lib"
            artifactId = "kmapper-annotation"
            version = "1.0"

            from(components["kotlin"])
        }
    }
}