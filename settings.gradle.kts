pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "truckie"

include(
    ":app",
    ":core-routing",
    ":core-profiles",
    ":core-trips",
    ":core-maps",
    ":core-flags",
)
