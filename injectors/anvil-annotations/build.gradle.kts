plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}

dependencies {
    api("com.squareup.anvil:annotations:2.4.8")
    api("com.google.dagger:dagger:2.51.1")
}
