plugins {
    id("kmap.kotlin")
    id("kmap.mvn-publish")

    id("com.google.devtools.ksp").version("${Versions.kotlin}-${Versions.ksp}")

    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    api(project(":api"))
    api(project(":converter-api"))

    api(symbolProcessingApi)

    api(kotlinPoet)
    implementation(kotlinPoetKsp)

    // auto service
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.0.0")

    testImplementation(project(":api"))
    testImplementation(project(":converter"))
    testImplementation(kotlinTest)
    testImplementation(kotlinReflect)
    testImplementation(kotlinCompilerEmbeddable)
    testImplementation(symbolProcessing)
    testImplementation(kotlinCompileTesting)
    testImplementation(kotlinCompileTestingKsp)
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.jUnit}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.jUnit}")
}

ksp {
    arg("autoserviceKsp.verify", "true")
}

tasks.test {
    useJUnitPlatform()
}