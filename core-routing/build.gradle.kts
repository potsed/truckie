plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core-profiles"))
    implementation(project(":core-trips"))
    testImplementation(libs.junit)
}
