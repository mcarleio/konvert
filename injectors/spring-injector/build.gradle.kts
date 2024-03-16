plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
    id("com.google.devtools.ksp").version("${Versions.kotlin}-${Versions.ksp}")
}


dependencies {
    api(project(":plugin-api"))
    api(project(":injectors:spring-annotations"))

    // auto service
    implementation(autoServiceAnnotations)
    ksp(autoServiceKsp)

    testImplementation(project(":annotations"))
    testImplementation(project(":converter"))
    testImplementation(project(":processor"))
    testImplementation(testFixtures(project(":processor")))
    testImplementation(kotlinTest)
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.jUnit}")

}


tasks.test {
    useJUnitPlatform()
}
