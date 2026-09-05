import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "dev.brewkits.krelay.ComposeApp")
            // Export krelay so that its types (RelayFeature, ToastFeature, KRelayIosHelperKt, etc.)
            // are accessible from Swift via `import ComposeApp` without needing a separate
            // `import Krelay`. This avoids the "unable to resolve module dependency" error
            // when the iosApp target only links ComposeApp.framework.
            export(project(":krelay"))
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)

            // Android-specific integrations
            implementation(libs.playcore.review)
            implementation(libs.playcore.review.ktx)
            implementation(libs.androidx.biometric)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Material Icons Extended (for AutoMirrored icons and additional icons)
            implementation(compose.materialIconsExtended)

            // Voyager - Navigation library for KMP (used for some screens alongside Decompose)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)

            // Decompose - Alternative navigation library for KMP
            implementation(libs.decompose)
            implementation(libs.decompose.compose)

            // Moko libraries for KMP
            implementation(libs.moko.permissions)
            implementation(libs.moko.permissions.compose)
            implementation(libs.moko.biometry)
            implementation(libs.moko.biometry.compose)

            // Peekaboo - Image picker for KMP
            implementation(libs.peekaboo.ui)
            implementation(libs.peekaboo.image.picker)

            // KRelay library — api() so krelay types can be export()-ed in the iOS framework
            api(project(":krelay"))
            implementation(project(":krelay-compose"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "dev.brewkits.krelay.demo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.brewkits.krelay.demo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2
        versionName = "2.1.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

