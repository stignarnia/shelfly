plugins {
  id("com.android.library")
  id("kotlin-parcelize")
  id("dagger.hilt.android.plugin")
}

android {

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  buildFeatures {
    viewBinding = true
  }

  defaultConfig {
    minSdk = rootProject.extra["minSdk"] as Int
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  testOptions {
    // targetSdk only affects instrumentation tests in a library, and AGP 9 removed it from defaultConfig.
    targetSdk = rootProject.extra["targetSdk"] as Int
  }

  namespace = "xyz.stignarnia.ui_base"
}

// ThemeResourcesTest reads the theme XML off the filesystem rather than through R, because what it checks is which entries exist - something R cannot express.
// Gradle has no way to know that, so without this the test task stays up to date through any resource edit and the guard silently stops running.
tasks.withType<Test>().configureEach {
  inputs
    .dir("src/main/res")
    .withPropertyName("themeResources")
    .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
  implementation(project(":common"))
  implementation(project(":data-remote"))
  implementation(project(":data-local"))
  implementation(project(":ui-model"))
  implementation(project(":ui-navigation"))
  implementation(project(":repository"))

  testImplementation(libs.bundles.testing)

  api(libs.android.appcompat)
  api(libs.android.core)
  api(libs.bundles.android.lifecycle)
  api(libs.bundles.android.navigation)
  api(libs.android.fragment)
  api(libs.android.recycler)
  api(libs.android.constraintlayout)
  api(libs.android.work)
  api(libs.android.material)
  api(libs.android.dynamicanimation)
  api(libs.overscrollDecor)
  api(libs.timber)

  api(libs.glide)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.hilt.work)
  ksp(libs.hilt.work.compiler)

  coreLibraryDesugaring(libs.android.desugar)
}
