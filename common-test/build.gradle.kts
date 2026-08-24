plugins {
  id("com.android.library")
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
  packaging {
    resources {
      excludes += setOf("META-INF/*.md")
    }
  }

  testOptions {
    // targetSdk only affects instrumentation tests in a library, and AGP 9 removed it from defaultConfig.
    targetSdk = rootProject.extra["targetSdk"] as Int
  }

  namespace = "xyz.stignarnia.test_base"
}

dependencies {
  implementation(project(":common"))

  implementation(libs.coroutinesTest)
  implementation(libs.junit)
  coreLibraryDesugaring(libs.android.desugar)
}
