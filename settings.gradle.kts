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

rootProject.name = "Sanctuary"

include(
    ":app",
    ":core:model",
    ":core:database",
    ":core:data",
    ":core:designsystem",
    ":core:tracking",
    ":core:sync",
    ":feature:onboarding",
    ":feature:dashboard",
    ":feature:history",
    ":feature:profile",
)
