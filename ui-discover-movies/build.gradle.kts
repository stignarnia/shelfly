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

  namespace = "xyz.stignarnia.ui_discover_movies"
}

dependencies {
  implementation(project(":common"))
  implementation(project(":data-local"))
  implementation(project(":ui-base"))
  implementation(project(":data-remote"))
  implementation(project(":repository"))
  implementation(project(":ui-model"))
  implementation(project(":ui-navigation"))

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  coreLibraryDesugaring(libs.android.desugar)
}
