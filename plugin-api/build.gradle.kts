plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}


dependencies {
    api(symbolProcessingApi)
    api(kotlinPoet)
    api(kotlinPoetKsp)
    api(project(":converter-api"))

}
