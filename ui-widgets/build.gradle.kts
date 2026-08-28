plugins {
  id("com.android.library")
  id("dagger.hilt.android.plugin")
}

android {
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  defaultConfig {
    minSdk =
      libs.versions.minSdk
        .get()
        .toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  testOptions {
    // targetSdk only affects instrumentation tests in a library, and AGP 9 removed it from defaultConfig.
    targetSdk =
      libs.versions.targetSdk
        .get()
        .toInt()
  }

  namespace = "xyz.stignarnia.uiWidgets"
}

// WidgetPaletteResourcesTest reads the palette XML off the filesystem rather than through R, because what it checks is that two sets of colours agree - something R cannot express.
// Gradle has no way to know that, so without this the test task stays up to date through any resource edit and the guard silently stops running.
// The same reasoning, and the same block, as in ui-base.
tasks.withType<Test>().configureEach {
  inputs
    .dir("src/main/res")
    .withPropertyName("widgetPaletteResources")
    .withPathSensitivity(PathSensitivity.RELATIVE)
  inputs
    .dir("../ui-base/src/main/res")
    .withPropertyName("themeResources")
    .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
  implementation(project(":common"))
  implementation(project(":ui-base"))
  implementation(project(":repository"))
  implementation(project(":ui-model"))
  implementation(project(":ui-episodes"))
  implementation(project(":ui-progress"))
  implementation(project(":ui-progress-movies"))

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  testImplementation(libs.bundles.testing)

  coreLibraryDesugaring(libs.android.desugar)
}
