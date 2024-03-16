plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
    id("com.google.devtools.ksp").version("${Versions.kotlin}-${Versions.ksp}")
}

dependencies {
    api(project(":annotations"))
    api(project(":converter-api"))
    api(symbolProcessingApi)

    // auto service
    implementation(autoServiceAnnotations)
    ksp(autoServiceKsp)

    testImplementation(project(":processor"))
    testImplementation(testFixtures(project(":processor")))
    testImplementation(kotlinTest)
    testImplementation(kotlinReflect)
    testImplementation("com.github.dpaukov:combinatoricslib3:${Versions.combinatoricslib3}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.jUnit}")
    testImplementation("org.reflections:reflections:${Versions.orgReflections}")
}

ksp {
    arg("autoserviceKsp.verify", "true")
}

tasks.test {
    useJUnitPlatform {
        if (project.hasProperty("runAllTests")) {
            // gradle test -PrunAllTests
            includeTags = setOf("detailed", "none()")
        } else {
            excludeTags = setOf("detailed")
        }
    }
    maxParallelForks = 1.coerceAtLeast(Runtime.getRuntime().availableProcessors() / 2)
}
