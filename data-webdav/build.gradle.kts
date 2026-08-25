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

  namespace = "xyz.stignarnia.data_webdav"
}

dependencies {
  implementation(project(":common"))

  api(libs.okhttp)

  implementation(libs.coroutines)
  implementation(libs.timber)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.mockwebserver)
  testImplementation(libs.coroutinesTest)

  coreLibraryDesugaring(libs.android.desugar)
}
