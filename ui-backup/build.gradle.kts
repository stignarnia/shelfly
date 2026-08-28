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
    buildConfig = true
  }

  defaultConfig {
    minSdk =
      libs.versions.minSdk
        .get()
        .toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  namespace = "xyz.stignarnia.uiBackup"

  testOptions {
    // targetSdk only affects instrumentation tests in a library, and AGP 9 removed it from defaultConfig.
    targetSdk =
      libs.versions.targetSdk
        .get()
        .toInt()
    unitTests.all { test ->
      // BackupMigrationV2FileTest self-skips unless this points at a real v2 export.
      // Forwarded explicitly so it survives the Gradle daemon.
      test.environment("SHELFLY_V2_BACKUP", System.getenv("SHELFLY_V2_BACKUP") ?: "")
    }
  }
}

dependencies {
  implementation(project(":common"))
  implementation(project(":data-local"))
  implementation(project(":data-remote"))
  implementation(project(":data-webdav"))
  implementation(project(":repository"))
  implementation(project(":ui-base"))
  implementation(project(":ui-model"))

  implementation(libs.moshi)
  ksp(libs.moshi.codegen)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.android.work)
  implementation(libs.hilt.work)
  ksp(libs.hilt.work.compiler)

  api(libs.phoenix)

  testImplementation(project(":common-test"))
  testImplementation(libs.bundles.testing)
  testImplementation(libs.mockwebserver)

  coreLibraryDesugaring(libs.android.desugar)
}
