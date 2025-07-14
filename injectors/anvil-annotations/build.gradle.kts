plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}

dependencies {
    api("com.squareup.anvil:annotations:2.6.0")
    api("com.google.dagger:dagger:2.56.1")
}
