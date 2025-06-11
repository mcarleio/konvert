import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-library`
    id("com.vanniktech.maven.publish")
    signing
}

group = "io.mcarle"
version = System.getenv("RELEASE_VERSION") ?: "0.1.0-SNAPSHOT"

mavenPublishing {
    configure(KotlinJvm())

    if (System.getenv("CI") != null) {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()
    }

    val artifactId = if (project.name.contains("konvert")) {
        project.name
    } else {
        "konvert-${project.name}"
    }
    coordinates(null, artifactId)

    pom {
        name.set(artifactId)
        description.set("Konvert is a KSP to generate mapping code between types")
        url.set("https://github.com/mcarleio/konvert")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                name.set("Marcel Carlé")
                url.set("https://mcarle.io")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/mcarleio/konvert.git")
            developerConnection.set("scm:git:ssh://github.com:mcarleio/konvert.git")
            url.set("https://github.com/mcarleio/konvert")
        }

        inceptionYear.set("2023")
    }
}

publishing {
    if (System.getenv("CI") != null && System.getenv("GITHUB_TOKEN") != null) {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/mcarleio/konvert")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
