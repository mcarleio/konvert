plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}


dependencies {
    api(project(":api"))
    api(symbolProcessingApi)
}
