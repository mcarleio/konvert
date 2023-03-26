plugins {
    id("kmap.kotlin")
    id("kmap.mvn-publish")
}

dependencies {
    api(project(":converter-api"))
}