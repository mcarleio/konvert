plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
    id("com.google.devtools.ksp").version("${Versions.kotlin}-${Versions.ksp}")
    id("org.jetbrains.kotlinx.kover")
}


dependencies {
    api(project(":processor-api"))
    api(project(":injectors:spring-annotations"))

    // auto service
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.0.0")

    testImplementation(project(":api"))
    testImplementation(project(":converter"))
    testImplementation(project(":processor"))
    testImplementation(testFixtures(project(":processor")))
    testImplementation(kotlinTest)
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.jUnit}")

}


kover {
    useKoverTool()
    disabledForProject = System.getenv("CI") == null
}

tasks.test {
    useJUnitPlatform()
}
