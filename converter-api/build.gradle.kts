plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}


dependencies {
    api(project(":annotations"))
    api(symbolProcessingApi)
    api(kotlinPoet)

    testImplementation(kotlinTest)
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.jUnit}")
}

tasks.test {
    useJUnitPlatform()
}
