plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
    id("java-test-fixtures")
    id("com.google.devtools.ksp").version("${Versions.kotlin}-${Versions.ksp}")
}

dependencies {
    api(project(":annotations"))
    api(project(":plugin-api"))
    api(project(":converter-api"))

    api(symbolProcessingApi)

    api(kotlinPoet)
    implementation(kotlinPoetKsp)

    // auto service
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.1.0")

    testImplementation(project(":annotations"))
    testImplementation(project(":converter"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.jUnit}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.jUnit}")
    testImplementation(kotlinTest)
    testFixturesApi(kotlinCompileTesting)
    testFixturesApi(kotlinCompileTestingKsp)
    testFixturesApi(kotlinCompilerEmbeddable)
    testFixturesApi(symbolProcessing)
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.jUnit}")
}

ksp {
    arg("autoserviceKsp.verify", "true")
}

tasks.test {
    useJUnitPlatform()
}

kover {
    excludeSourceSets {
        names(sourceSets.testFixtures.name)
    }
}
