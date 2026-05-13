import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
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
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = JvmTarget.JVM_17
                }
            }
        }
    }
    js {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    // Native targets - Tier 1
    linuxX64()
    macosX64()
    macosArm64()

    // Native targets - Tier 2 (iOS)
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    // Native targets - Tier 2 (watchOS, tvOS)
    watchosArm64()
    watchosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()

    // Native targets - Tier 3
    mingwX64()
    linuxArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(kotlinStdlib)
            }
        }
    }
}
