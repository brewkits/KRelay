import java.util.Base64

plugins {
    id("java-platform")
    id("maven-publish")
    id("signing")
}

group = "dev.brewkits"
version = "2.2.0"

// Declare BOM constraints — all KRelay artifacts aligned to the same version
dependencies {
    constraints {
        api(project(":krelay"))
        api(project(":krelay-compose"))
        api(project(":krelay-testing"))
        api(project(":krelay-flow"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenBom") {
            groupId = "dev.brewkits"
            artifactId = "krelay-bom"
            version = project.version.toString()

            from(components["javaPlatform"])

            pom {
                name.set("KRelay BOM")
                description.set("Bill of Materials for KRelay. Import this BOM to align all krelay artifact versions automatically.")
                url.set("https://github.com/brewkits/krelay")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("brewkits")
                        name.set("BrewKits Dev Team")
                        email.set("dev@brewkits.dev")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/brewkits/krelay.git")
                    developerConnection.set("scm:git:ssh://github.com/brewkits/krelay.git")
                    url.set("https://github.com/brewkits/krelay")
                }
            }
        }
    }

    repositories {
        maven {
            name = "MavenCentralLocal"
            url = uri("${rootProject.layout.buildDirectory.get()}/maven-central-staging")
        }

        maven {
            name = "OSSRH"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME")
                password = findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    val rawKey = findProperty("signing.key")?.toString() ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signing.password")?.toString() ?: System.getenv("SIGNING_PASSWORD")
    if (rawKey != null && signingPassword != null) {
        val decoded = try {
            val d = String(Base64.getDecoder().decode(rawKey))
            if (d.contains("-----BEGIN PGP")) d else rawKey
        } catch (_: Exception) { rawKey }
        useInMemoryPgpKeys(decoded, signingPassword)
        sign(publishing.publications["mavenBom"])
    } else if (findProperty("signing.keyId") != null) {
        sign(publishing.publications["mavenBom"])
    }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(tasks.withType<Sign>())
}
