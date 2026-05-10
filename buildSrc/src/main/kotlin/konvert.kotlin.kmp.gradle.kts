import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") // version defined in buildSrc/build.gradle.kts -> dependencies
    id("org.jetbrains.kotlinx.kover")
}

dependencies {

}

repositories {
    mavenCentral()
}

kover {
    if (System.getenv("CI") == null) {
        disable()
    }
}

kotlin {
    jvm {
        compilations.all {
            compilerOptions.configure {
                jvmTarget = JvmTarget.JVM_17
            }
        }
    }
    js {
        browser()
        nodejs()
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(kotlinStdlib)
            }
        }
    }
}
