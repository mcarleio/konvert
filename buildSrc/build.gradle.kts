plugins {
    `kotlin-dsl`
}
dependencies {
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:${property("kotlin.version")}")
    implementation("org.jetbrains.kotlinx.kover:org.jetbrains.kotlinx.kover.gradle.plugin:${property("kover.version")}")
    implementation("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:${property("mavenPublish.version")}")
}

repositories {
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
    mavenCentral()
}
