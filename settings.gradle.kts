pluginManagement {
  // Plugins are resolved through the plugins {} block rather than a buildscript classpath, so their markers have to be reachable here.
  // AGP, Hilt and Room live on google() and mavenCentral(); the portal is only the fallback for the toolchain resolver below.
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  // Lets Gradle download the JDK the toolchain asks for, so the build does not depend on which JDKs happen to be installed locally or on CI.
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Shelfly"
include(":app")
include(":common")
include(":common-test")
include(":data-local")
include(":data-remote")
include(":data-webdav")
include(":repository")
include(":ui-backup")
include(":ui-base")
include(":ui-discover")
include(":ui-discover-movies")
include(":ui-episodes")
include(":ui-gallery")
include(":ui-lists")
include(":ui-model")
include(":ui-movie")
include(":ui-my-movies")
include(":ui-my-shows")
include(":ui-navigation")
include(":ui-people")
include(":ui-progress")
include(":ui-progress-movies")
include(":ui-search")
include(":ui-settings")
include(":ui-show")
include(":ui-statistics")
include(":ui-statistics-movies")
include(":ui-streamings")
include(":ui-widgets")
