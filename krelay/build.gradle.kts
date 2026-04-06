import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URL
import java.util.Base64

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
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
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            implementation("org.jetbrains.kotlinx:atomicfu:0.23.2")
        }
        androidMain.dependencies {
            // Android specific dependencies if needed
        }
        // Instrumentation tests — run on real device/emulator: ./gradlew :krelay:connectedDebugAndroidTest
        androidInstrumentedTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.espresso.core)
            implementation("org.jetbrains.kotlinx:atomicfu:0.23.2")
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

        // Run instrumented tests: ./gradlew :krelay:connectedDebugAndroidTest
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Generate Dokka HTML docs: ./gradlew :krelay:dokkaHtml
// Output: krelay/build/dokka/html/index.html
tasks.register<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtmlCustom") {
    outputDirectory.set(rootProject.file("docs/api"))
    moduleName.set("KRelay")
    dokkaSourceSets.configureEach {
        includeNonPublic.set(false)
        skipDeprecated.set(false)
        reportUndocumented.set(true)
        skipEmptyPackages.set(true)
        sourceLink {
            localDirectory.set(file("src/commonMain/kotlin"))
            remoteUrl.set(URL("https://github.com/brewkits/krelay/blob/main/krelay/src/commonMain/kotlin"))
            remoteLineSuffix.set("#L")
        }
    }
}

// ---------------------------------------------------------------------------
// Maven Central compliance: every publication must carry a -javadoc.jar.
// KMP native/metadata targets don't produce real Javadoc, so we publish an
// empty placeholder (same pattern used by kotlinx libraries).
// ---------------------------------------------------------------------------
val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    // deliberately empty — Dokka HTML lives in docs/api/, not here
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
            version = project.version.toString()

            // Attach javadoc JAR to every publication (Maven Central requires it)
            artifact(emptyJavadocJar)

            pom {
                name.set("KRelay")
                description.set("The missing piece in Kotlin Multiplatform. Safely dispatch UI events (Toasts, Navigation, Permissions) from shared ViewModels to Android/iOS — zero memory leaks, sticky queue, always on Main Thread.")
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
    val rawKey = findProperty("signing.key")?.toString() ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signing.password")?.toString() ?: System.getenv("SIGNING_PASSWORD")

    if (rawKey != null && signingPassword != null) {
        // Key may be stored as base64 in gradle.properties — decode if needed
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

// Fix implicit dependency ordering issue between signing and publishing tasks
tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(tasks.withType<Sign>())
}
