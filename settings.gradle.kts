pluginManagement {
    repositories {
        // Simplemente declaramos los repositorios. Sin filtros complicados.
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Esta es la parte crucial para las dependencias de la app.
        google()
        mavenCentral()
    }
}

rootProject.name = "despertador1"
include(":app")
