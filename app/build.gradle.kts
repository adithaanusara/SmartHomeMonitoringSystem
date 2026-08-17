import java.util.Properties

/**
 * Release signing credentials, kept out of the repository.
 *
 * When keystore.properties is absent — a teammate's fresh clone, or CI — the release build simply
 * has no signing config and produces an unsigned APK, rather than failing the whole configuration
 * phase and taking debug builds down with it.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.smarthomeapp"
    // androidx.core 1.19.0 and lifecycle 2.11.0 require compiling against API 37+.
    // targetSdk stays at 36 — bumping it opts into new runtime behaviour we don't need.
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.smarthomeapp"
        minSdk = 26
        targetSdk = 36
        // v1.2 — UI/UX pass: a fixed brand palette in place of the template's dynamic colour,
        // a full type scale, shared spacing/shape tokens, and status-driven motion.
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            // R8 shrinking and obfuscation. The Firebase model classes are held by the keep rules
            // in src/main/keepRules — without those, reflective deserialisation silently returns
            // empty objects in release only.
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation + Compose-aware lifecycle
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Firebase — BOM pins the versions of the artifacts below
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.kotlinx.coroutines.play.services)

    // Mock camera snapshots / floor plan images loaded from URLs
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}