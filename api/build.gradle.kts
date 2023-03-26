plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}

dependencies {
    api(project(":converter-api"))
}
