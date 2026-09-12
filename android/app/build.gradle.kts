plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.coursecraft.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.coursecraft.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Backend base URL — overridable per environment via -PBACKEND_BASE_URL=...
        val backendBaseUrl = (project.findProperty("BACKEND_BASE_URL") as String?)
            ?: "http://10.0.2.2:8080"
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")

        // Supabase Auth (publishable key is public by design).
        val supabaseUrl = (project.findProperty("SUPABASE_URL") as String?) ?: ""
        val supabaseKey = (project.findProperty("SUPABASE_PUBLISHABLE_KEY") as String?) ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabaseKey\"")
    }

    // Release signing is driven by env vars so no secrets live in the repo. When they are absent
    // (local dev), the release build stays unsigned and CI signs it from GitHub Actions secrets.
    val keystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Robust YouTube playback (IFrame Player API) instead of a raw WebView.
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Native playback for self-hosted MP4/HLS (plays inline on any device, incl. old ones).
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
