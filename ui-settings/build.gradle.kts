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
    buildConfig = true
    viewBinding = true
  }

  defaultConfig {
    minSdk =
      libs.versions.minSdk
        .get()
        .toInt()

    buildConfigField("int", "VER_CODE", "${libs.versions.versionCode.get()}")
    buildConfigField("String", "VER_NAME", "\"${libs.versions.versionName.get()}\"")

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

  namespace = "xyz.stignarnia.uiSettings"
}

dependencies {
  implementation(project(":common"))
  implementation(project(":data-local"))
  implementation(project(":data-remote"))
  implementation(project(":data-webdav"))
  implementation(project(":repository"))
  implementation(project(":ui-base"))
  implementation(project(":ui-backup"))
  implementation(project(":ui-model"))
  implementation(project(":ui-navigation"))

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  api(libs.phoenix)

  coreLibraryDesugaring(libs.android.desugar)
}
