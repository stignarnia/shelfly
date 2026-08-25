plugins {
  id("com.android.library")
  id("dagger.hilt.android.plugin")
  id("androidx.room")
}

room {
  // Room writes the schema it expects here, so hand-written migrations can be checked against it rather than discovered to be wrong at runtime.
  schemaDirectory("$projectDir/schemas")
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

  namespace = "xyz.stignarnia.data_local"
}

dependencies {
  api(libs.android.room.ktx)
  api(libs.android.room.runtime)
  ksp(libs.android.room.compiler)

  implementation(libs.timber)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  testImplementation(libs.junit)
  androidTestImplementation(libs.truth)
  androidTestImplementation(libs.android.test.runner)
  androidTestImplementation(libs.android.test.ext.junit)
  androidTestImplementation(libs.android.test.truth)
  androidTestImplementation(libs.android.room.testing)

  coreLibraryDesugaring(libs.android.desugar)
}
