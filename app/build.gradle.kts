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
        versionName = "0.8.50-valhalla-oci"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Live Valhalla credentials (ATNA-113 / ATNA-9). Empty → stub PathRouter.
        // Interim: API key (defaults base to Simple Routing hosted Valhalla).
        // Self-hosted (OCI Free Tier / compose): set VALHALLA_BASE_URL, clear API key.
        // Priority: -P gradle → env → secrets.properties → local.properties
        // See docs/valhalla-live.md, docs/oci-valhalla-runbook.md.
        fun loadPropertiesFile(file: java.io.File): Map<String, String> {
            if (!file.exists()) return emptyMap()
            return file.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                .associate { line ->
                    val idx = line.indexOf('=')
                    line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                }
        }
        val secretsMap = loadPropertiesFile(rootProject.file("secrets.properties"))
        // local.properties is gitignored (SDK path); also accepts VALHALLA_* overrides.
        val localPropsMap = loadPropertiesFile(rootProject.file("local.properties"))
        fun secretOrEnv(propKey: String, envKey: String, gradleProp: String): String {
            val fromGradle = (project.findProperty(gradleProp) as String?)?.trim().orEmpty()
            if (fromGradle.isNotEmpty()) return fromGradle
            val fromEnv = System.getenv(envKey)?.trim().orEmpty()
            if (fromEnv.isNotEmpty()) return fromEnv
            val fromSecrets = secretsMap[propKey]?.trim().orEmpty()
            if (fromSecrets.isNotEmpty()) return fromSecrets
            return localPropsMap[propKey]?.trim().orEmpty()
        }
        fun escapeBuildConfig(value: String): String =
            value.replace("\\", "\\\\").replace("\"", "\\\"")

        val valhallaBaseUrl = escapeBuildConfig(
            secretOrEnv(
                propKey = "VALHALLA_BASE_URL",
                envKey = "TRUCKIE_VALHALLA_BASE_URL",
                gradleProp = "truckie.valhalla.baseUrl",
            ),
        )
        val valhallaApiKey = escapeBuildConfig(
            secretOrEnv(
                propKey = "VALHALLA_API_KEY",
                envKey = "TRUCKIE_VALHALLA_API_KEY",
                gradleProp = "truckie.valhalla.apiKey",
            ),
        )
        buildConfigField("String", "VALHALLA_BASE_URL", "\"$valhallaBaseUrl\"")
        buildConfigField("String", "VALHALLA_API_KEY", "\"$valhallaApiKey\"")
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

// Room schema export for Migration / exportSchema = true (ATNA-89).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.maplibre.android)
    implementation(libs.maplibre.annotation)
    implementation(libs.play.services.location)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Android Auto (Cars App Library) — runtime gated to API 23+
    implementation(libs.androidx.car.app)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Durable feature-flag overrides (ATNA-93) — Preferences DataStore.
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
