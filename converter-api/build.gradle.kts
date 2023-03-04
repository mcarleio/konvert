plugins {
    id("java-library")
    kotlin("jvm") version "1.7.22"
    id("maven-publish")
}


dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("com.google.devtools.ksp:symbol-processing-api:1.7.22-1.0.8")
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
            artifactId = "kmapper-converter-api"
            version = "1.0"

            from(components["kotlin"])
        }
    }
}