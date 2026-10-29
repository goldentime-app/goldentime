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
        // Pleos Connect SDK (Vehicle, NaviHelper 등)
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
rootProject.name = "goldentime"
include(":app")
