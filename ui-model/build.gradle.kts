plugins {
  id("com.android.library")
  id("kotlin-parcelize")
}

android {
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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

  namespace = "xyz.stignarnia.ui_model"
}

dependencies {
  implementation(project(":common"))

  implementation(libs.android.appcompat)

  coreLibraryDesugaring(libs.android.desugar)
}
