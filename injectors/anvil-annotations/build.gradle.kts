plugins {
    id("konvert.kotlin")
    id("konvert.mvn-publish")
}

dependencies {
    api("com.squareup.anvil:annotations:2.4.5")
    api("com.google.dagger:dagger:2.46")
}
