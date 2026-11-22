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
        // The Marpokat chat SDK lives in GitHub Packages. Reading needs a token with read:packages: set gpr.user and
        // gpr.token in ~/.gradle/gradle.properties, or MARPOKAT_PACKAGES_TOKEN in the environment (CI secret PACKAGES_TOKEN).
        maven {
            url = uri("https://maven.pkg.github.com/rizkysiregar/marpokat")
            credentials {
                username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.token").orElse(providers.environmentVariable("MARPOKAT_PACKAGES_TOKEN")).orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
            content { includeGroup("com.rizkysiregar.marpokat") }
        }
        // -Pmarpokat.local: use an SDK built on this machine (./gradlew publishToMavenLocal in marpokat/sdk-android)
        if (providers.gradleProperty("marpokat.local").isPresent) {
            mavenLocal { content { includeGroup("com.rizkysiregar.marpokat") } }
        }
    }
}

rootProject.name = "narrativ-android"
include(":app")
