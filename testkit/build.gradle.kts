plugins {
    id("konvert.kotlin")
}

dependencies {
    testImplementation(kotlinTest)
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.jUnit}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${Versions.jUnit}")
}

val testkitCoverageDir = layout.buildDirectory.dir("kover/testkit-bin-reports")

tasks.test {
    // The testkit tests are slow (they publish nearly all modules and run real Gradle TestKit builds),
    // so they are skipped during regular builds. They run when either the `runAllTests` property is set
    // (used on CI) or when the test task is explicitly requested - e.g. running `:testkit:test`
    val explicitlyRequested = gradle.startParameter.taskNames.any {
        it == path || it == path.removePrefix(":")
    }

    if (!project.hasProperty("runAllTests") && !explicitlyRequested) {
        enabled = false

        // To prevent adding unnecessary dependsOn relations, skip the configuration if it is not going to be run
        return@test
    }


    // Ensure (nearly) all modules are built and published to test repository
    dependsOn(
        rootProject.allprojects.mapNotNull { project ->
            if (project.name in listOf(this.project.name, "docs")) return@mapNotNull null
            project.tasks.findByName(TestkitRepo.PUBLISH_TASK)
        }
    )

    useJUnitPlatform()

    doFirst {
        systemProperty("testkitTests.maven.test.repo.dir", TestkitRepo.dir(rootProject).get().asFile.absolutePath)
        systemProperty("testkitTests.konvert.version", rootProject.version)
        systemProperty("testkitTests.kotlin.version", Versions.kotlin)
        systemProperty("testkitTests.ksp.version", Versions.ksp)
    }
    if (isCI()) {
        val koverAgentConfig = configurations.named("koverJvmAgent")
        doFirst {
            val agentJar = koverAgentConfig.get().singleFile
            systemProperty("testkitTests.kover.agent.jar", agentJar.absolutePath)
            systemProperty("testkitTests.kover.report.dir", testkitCoverageDir.get().asFile.absolutePath)
        }
    }
}

kover {
    reports {
        total {
            val testKitBinaryReports = providers.provider {
                testkitCoverageDir
                    .get()
                    .asFile
                    .walkTopDown()
                    .filter { it.isFile && it.extension == "ic" }
                    .toSet()
            }
            additionalBinaryReports.addAll(testKitBinaryReports)
        }
    }
}
