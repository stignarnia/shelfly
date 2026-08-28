// Top-level build file where you can add configuration options common to all sub-projects/modules.
import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

// Declared here so every module can apply them by id without repeating a version.
// The version catalog is reachable from plugins {} but not from a buildscript classpath block, which is why these are aliases rather than classpath entries.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.android.room) apply false
  alias(libs.plugins.devtools.ksp) apply false
  alias(libs.plugins.hilt.android) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.parcelize) apply false
}

allprojects {
  apply(plugin = "com.google.devtools.ksp")

  repositories {
    google()
    mavenCentral()
  }
}

// Read once here: the catalog accessor is not available inside the subprojects block.
val compileSdkVersion =
  libs.versions.compileSdk
    .get()
    .toInt()
val buildToolsRelease = libs.versions.buildTools.get()
val jvmTargetVersion =
  libs.versions.jvmTarget
    .get()
    .toInt()

subprojects {
  // Centralised so the SDK and bytecode level live in one place instead of being repeated in every module's android {} block.
  // AGP's built-in Kotlin support creates the kotlin extension itself, so this hooks com.android.base rather than org.jetbrains.kotlin.android - the latter is never applied and a block keyed on it never runs.
  plugins.withId("com.android.base") {
    extensions.configure<CommonExtension>("android") {
      compileSdk = compileSdkVersion
      buildToolsVersion = buildToolsRelease

      // Set through the property rather than a lint {} block: CommonExtension carries no type arguments here, so the Action overload does not resolve.
      // Lint skips test sources by default, so test code is held to no standard at all.
      // Enabling it costs nothing today - the test sources report zero findings - and keeps it that way as tests are added.
      lint.checkTestSources = true
      lint.warningsAsErrors = true

      // Two checks that are off by default and are named individually rather than through checkAllWarnings.
      //
      // StringFormatTrivial catches String.format("%s", x), which only calls toString() while allocating a Formatter and parsing the format string at runtime.
      //
      // ConvertToWebp catches a PNG that would be smaller as lossless WebP.
      lint.enable += setOf("StringFormatTrivial", "ConvertToWebp")
    }

    extensions.configure<KotlinAndroidProjectExtension>("kotlin") {
      // Gradle itself runs on whatever JDK is installed; compilation is pinned to the toolchain so the output does not change with the local JDK.
      jvmToolchain(jvmTargetVersion)

      compilerOptions {
        // The tree compiles warning-free, so warnings are errors to keep it that way.
        allWarningsAsErrors = true
      }
    }
  }
}

tasks.register<Delete>("clean") {
  delete(layout.buildDirectory)
}
