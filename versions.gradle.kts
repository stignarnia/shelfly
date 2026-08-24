// Single source of truth for the version and the SDK levels.
//
// Applied once, by the root build script, and read from modules as rootProject.extra["minSdk"] and friends.
// Do not apply it from a module: lint crashes analysing a build script that applies a Kotlin script, and swallows the crash, so the module's build file is silently never checked again.
//
// Read by scripts/check-config.sh and scripts/check-release-notes.sh as well as by Gradle, so the "versionName" line is parsed literally - keep its shape.
extra["versionCode"] = 7
extra["versionName"] = "4.0.6"

extra["minSdk"] = 23
extra["compileSdk"] = 37
extra["targetSdk"] = 37

extra["buildTools"] = "37.0.0"
// Bytecode target.
// Stays at 21 (Android's ceiling) regardless of the JDK running Gradle - see the toolchain block in the root build.gradle.kts.
extra["jvmTarget"] = 21
