import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.manumnoha.healthbridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.manumnoha.healthbridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "SERVER_URL", "\"${localProps["serverUrl"] ?: ""}\"")
        buildConfigField("String", "API_KEY", "\"${localProps["apiKey"] ?: ""}\"")
        buildConfigField("String", "NIGHTSCOUT_URL", "\"${localProps["nightscoutUrl"] ?: ""}\"")
        buildConfigField("String", "NIGHTSCOUT_TOKEN", "\"${localProps["nightscoutToken"] ?: ""}\"")
        buildConfigField("String", "DEXCOM_USERNAME", "\"${localProps["dexcomUsername"] ?: ""}\"")
        buildConfigField("String", "DEXCOM_PASSWORD", "\"${localProps["dexcomPassword"] ?: ""}\"")
        buildConfigField("String", "WODIFY_API_KEY", "\"${localProps["wodifyApiKey"] ?: ""}\"")
        buildConfigField("String", "WODIFY_ATHLETE_ID", "\"${localProps["wodifyAthleteId"] ?: ""}\"")
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.3")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Health Connect — reads Samsung Health data synced by Galaxy Watch
    implementation("androidx.health.connect:connect-client:1.1.0-rc01")
}
