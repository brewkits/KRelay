import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Base64

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.bcv)
    id("maven-publish")
    id("signing")
}

group = "dev.brewkits"
version = "2.1.1"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    jvm() // Desktop target
    wasmJs {
        browser()
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KRelayCompose"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":krelay"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "dev.brewkits.krelay.compose"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// ---------------------------------------------------------------------------
// Maven Central compliance: every publication must carry a -javadoc.jar.
// ---------------------------------------------------------------------------
val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    // deliberately empty — Dokka HTML lives in docs/api/
}

publishing {
    publications {
        withType<MavenPublication> {
            groupId = "dev.brewkits"
            version = project.version.toString()

            artifact(emptyJavadocJar)

            pom {
                name.set("KRelay Compose")
                description.set("Compose Multiplatform helpers for KRelay — lifecycle-aware KRelayEffect and rememberKRelayImpl composables.")
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
            url = uri("${layout.buildDirectory.get()}/maven-central-staging")
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
        val signingKey = try {
            val decoded = String(Base64.getDecoder().decode(rawKey))
            if (decoded.contains("-----BEGIN PGP")) decoded else rawKey
        } catch (_: Exception) { rawKey }
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else if (findProperty("signing.keyId") != null) {
        sign(publishing.publications)
    }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(tasks.withType<Sign>())
}
