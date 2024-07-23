import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `maven-publish`
    signing
}

publishing {
    publications.withType<MavenPublication> {
        // TODO finalize
        // Stub javadoc.jar artifact
        artifact(tasks.register("${name}JavadocJar", Jar::class) {
            archiveClassifier.set("javadoc")
            archiveAppendix.set(this@withType.name)
        })

        pom {
            name.set("Extended Numerics")
            description.set("Extended numerics types for Kotlin Multiplatform")
            url.set("https://github.com/aeckar/extended-numerics")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("aeckar")
                    name.set("Angel Eckardt")
                    organization.set("University of South Florida")
                    organizationUrl.set("https://www.usf.edu/")
                }
            }
            scm {
                url.set("https://github.com/aeckar/extended-numerics")
            }
        }
    }
}

signing {
    if (project.hasProperty("signing.gnupg.keyName")) {
        useGpgCmd()
        sign(publishing.publications)
    }
}
