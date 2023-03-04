plugins {
    kotlin("jvm") version "1.7.22"
    id("java-library")
    id("maven-publish")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly(project(":converter-api"))
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
            artifactId = "kmapper-api"
            version = "1.0"

            from(components["kotlin"])
        }
    }
}