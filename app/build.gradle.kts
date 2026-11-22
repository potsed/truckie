plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.truckie1.app"
    // AndroidX (core-ktx 1.19+, Compose BOM 2026.11+) requires compileSdk ≥ 37.1;
    // Studio recommends 37.2. Platform package: platforms;android-37.2
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.truckie1.app"
        // Compose BOM 2026.11 / Material3 1.5 declare minSdk 24 (Material 1.14+ / Cars App need 23+).
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.5.0-compose-ui"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Built-in Kotlin (AGP 9): jvmTarget defaults to compileOptions.targetCompatibility.
kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core-flags"))
    implementation(project(":core-profiles"))
    implementation(project(":core-trips"))
    implementation(project(":core-routing"))
    implementation(project(":core-maps"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Android Auto (Cars App Library) — runtime gated to API 23+
    implementation(libs.androidx.car.app)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
}
