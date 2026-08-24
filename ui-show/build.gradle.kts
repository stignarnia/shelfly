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

  namespace = "xyz.stignarnia.ui_show"
}

dependencies {
  implementation(project(":common"))
  implementation(project(":data-local"))
  implementation(project(":data-remote"))
  implementation(project(":repository"))
  implementation(project(":ui-base"))
  implementation(project(":ui-model"))
  implementation(project(":ui-navigation"))
  implementation(project(":ui-episodes"))
  implementation(project(":ui-streamings"))
  implementation(project(":ui-people"))

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  coreLibraryDesugaring(libs.android.desugar)
}
