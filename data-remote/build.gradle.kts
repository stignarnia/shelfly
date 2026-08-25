import java.util.Properties

plugins {
  id("com.android.library")
  id("dagger.hilt.android.plugin")
}

// local.properties is gitignored and absent on a fresh clone and on CI.
// Missing keys fall back to empty strings rather than failing the build.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
  localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  buildFeatures {
    buildConfig = true
  }

  defaultConfig {
    minSdk = libs.versions.minSdk
      .get()
      .toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes.all {
    buildConfigField("String", "VER_NAME", "\"${libs.versions.versionName.get()}\"")
    buildConfigField("int", "VER_CODE", "${libs.versions.versionCode.get()}")

    // Release ships no key material - users enter their own keys at runtime.
    // Debug builds prefill from local.properties so development does not mean retyping a key after every install.
    buildConfigField("String", "TMDB_API_KEY", "\"\"")
    buildConfigField("String", "OMDB_API_KEY", "\"\"")
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
      buildConfigField("String", "TMDB_API_KEY", localProperties.getProperty("tmdbApiKey", "\"\""))
      buildConfigField("String", "OMDB_API_KEY", localProperties.getProperty("omdbApiKey", "\"\""))
    }

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

  namespace = "xyz.stignarnia.data_remote"
}

dependencies {
  implementation(project(":common"))

  api(libs.retrofit)
  api(libs.retrofit.moshi)
  api(libs.loggingInterceptor)

  implementation(libs.moshi)
  ksp(libs.moshi.codegen)
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
