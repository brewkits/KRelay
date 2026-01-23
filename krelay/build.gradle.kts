import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
    id("signing")
}

group = "dev.brewkits"
version = "1.0.1"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    // iOS targets - publish each as separate artifact
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KRelay"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // No external dependencies - pure Kotlin stdlib
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            // Android specific dependencies if needed
        }
        iosMain.dependencies {
            // iOS specific dependencies if needed
        }
    }
}

android {
    namespace = "dev.brewkits.krelay"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Automatically apply consumer rules to apps using this library
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            groupId = "dev.brewkits"
            // Don't override artifactId - let Gradle use default naming:
            // - kotlinMultiplatform -> krelay
            // - androidRelease -> krelay-android
            // - iosArm64 -> krelay-iosarm64
            // - iosSimulatorArm64 -> krelay-iossimulatorarm64
            // - iosX64 -> krelay-iosx64
            // - js -> krelay-js
            version = project.version.toString()

            pom {
                name.set("KRelay")
                description.set("The Native Interop Bridge for Kotlin Multiplatform - Safe dispatch, weak registry, and sticky queue for seamless ViewModel-to-View communication")
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
        // Local staging repository for verification before publishing
        maven {
            name = "MavenCentralLocal"
            url = uri("${layout.buildDirectory.get()}/maven-central-staging")
        }

        // Maven Central (Sonatype OSSRH)
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
    // Support both in-memory key (signing.key) and keyring (signing.keyId)
    val signingKey = findProperty("signing.key")?.toString() ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signing.password")?.toString() ?: System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        // Use in-memory key (recommended for CI/CD)
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else if (findProperty("signing.keyId") != null) {
        // Use traditional GPG keyring
        sign(publishing.publications)
    }
}
