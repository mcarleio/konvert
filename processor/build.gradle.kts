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
    implementation(autoServiceAnnotations)
    ksp(autoServiceKsp)

    testImplementation(project(":annotations"))
    testImplementation(project(":converter"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.jUnit}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.jUnit}")
    testImplementation(kotlinTest)
    testImplementation(kotlinReflect)
    testFixturesApi(kotlinCompileTesting)
    testFixturesApi(kotlinCompileTestingKsp)
    testFixturesApi("com.google.devtools.ksp:symbol-processing-common-deps:${Versions.kotlin}-${Versions.ksp}")
    testFixturesApi("com.google.devtools.ksp:symbol-processing-aa-embeddable:${Versions.kotlin}-${Versions.ksp}")
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
    currentProject {
        sources {
            excludedSourceSets.addAll(sourceSets.testFixtures.name)
        }
    }
}

val javaComponent = components["java"] as AdhocComponentWithVariants
javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
