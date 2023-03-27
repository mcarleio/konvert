plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "io.mcarle"
version = System.getenv("RELEASE_VERSION") ?: "0.1.0-SNAPSHOT"

publishing {
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/mcarleio/konvert")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {

            if (project.name.contains("konvert")) {
                artifactId = project.name
            } else {
                artifactId = "konvert-${project.name}"
            }

            from(components["java"])

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
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

signing {
    useInMemoryPgpKeys(System.getenv("SIGN_KEYID"), System.getenv("SIGN_KEY"), System.getenv("SIGN_KEY_PASS"))
    sign(publishing.publications["maven"])
}
