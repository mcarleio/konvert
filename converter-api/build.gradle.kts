plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}


dependencies {
    api(project(":annotations"))
    api(symbolProcessingApi)
    api(kotlinPoet)
}
