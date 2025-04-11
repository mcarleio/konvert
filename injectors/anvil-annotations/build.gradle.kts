plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}

dependencies {
    api("com.squareup.anvil:annotations:2.5.1")
    api("com.google.dagger:dagger:2.52")
}
