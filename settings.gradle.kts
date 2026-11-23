pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "AiBoxCarLauncher"

include(":app")
include(":core:core-model")
include(":core:core-data")
include(":core:core-ui")
include(":launcher")
include(":desktop")
include(":dock")
include(":widgets")
include(":wallpaper")
include(":weather")
include(":control-center")
include(":app-drawer")
include(":settings")
include(":icons")
include(":gestures")
include(":system")
