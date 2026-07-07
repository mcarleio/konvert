import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") // version defined in buildSrc/build.gradle.kts -> dependencies
    id("org.jetbrains.kotlinx.kover")
}

repositories {
    mavenCentral()
}

kover {
    if (!isCI()) {
        disable()
    }
}

kotlin {
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = JvmTarget.JVM_17
                    javaParameters = true
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
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // Native targets: https://kotlinlang.org/docs/native-target-support.html
    // Native targets - Tier 1
    macosArm64()
    iosSimulatorArm64()
    iosArm64()

    // Native targets - Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosArm64()

    // Native targets - Tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()

    applyDefaultHierarchyTemplate()

}
