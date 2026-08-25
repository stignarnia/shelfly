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

  namespace = "xyz.stignarnia.repository"
}

dependencies {
  implementation(project(":common"))
  implementation(project(":data-remote"))
  implementation(project(":data-local"))
  implementation(project(":ui-model"))

  implementation(libs.android.core)
  implementation(libs.android.appcompat)
  implementation(libs.timber)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  testImplementation(project(":common-test"))
  testImplementation(libs.bundles.testing)
  androidTestImplementation(libs.android.test.runner)

  coreLibraryDesugaring(libs.android.desugar)
}
