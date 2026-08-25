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
    minSdk = libs.versions.minSdk
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
    targetSdk = libs.versions.targetSdk
      .get()
      .toInt()
  }

  namespace = "xyz.stignarnia.ui_progress"
}

dependencies {
  implementation(project(":common"))
  implementation(project(":data-local"))
  implementation(project(":ui-base"))
  implementation(project(":ui-backup"))
  implementation(project(":repository"))
  implementation(project(":ui-model"))
  implementation(project(":ui-navigation"))
  implementation(project(":ui-episodes"))

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  coreLibraryDesugaring(libs.android.desugar)
}
