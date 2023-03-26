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
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
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
