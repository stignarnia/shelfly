import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.util.Properties

plugins {
  id("com.android.application")
  id("com.google.devtools.ksp")
  id("dagger.hilt.android.plugin")
}

android {
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  androidResources {
    generateLocaleConfig = true
  }

  buildFeatures {
    buildConfig = true
    viewBinding = true
  }

  bundle {
    language {
      enableSplit = false
    }
  }

  defaultConfig {
    applicationId = "xyz.stignarnia.shelfly"
    minSdk =
      libs.versions.minSdk
        .get()
        .toInt()
    targetSdk =
      libs.versions.targetSdk
        .get()
        .toInt()
    versionCode =
      libs.versions.versionCode
        .get()
        .toInt()
    versionName = libs.versions.versionName.get()

    // Kept in step with the res/values-* directories by scripts/check-config.sh, which fails if either side gains a locale the other lacks.
    resourceConfigurations +=
      listOf(
        "en",
        "ar",
        "de",
        "da",
        "es",
        "fi",
        "fr",
        "it",
        "pl",
        "pt",
        "ro",
        "ru",
        "tr",
        "zh",
        "uk",
      )
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    // Both files are gitignored, so a fresh clone has neither.
    // Only declare the release signing config when they are actually present - otherwise every task, debug builds and unit tests included, fails at configuration time.
    val keystorePropertiesFile = rootProject.file("app/keystore.properties")
    val keystoreFile = file("keystore")

    if (keystorePropertiesFile.exists() && keystoreFile.exists()) {
      val keystoreProperties = Properties()
      keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }

      create("release") {
        storeFile = keystoreFile
        storePassword = keystoreProperties["storePassword"] as String
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
      }
    }
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debugoss"
      versionNameSuffix = "-debug"
      isMinifyEnabled = false
      // The debug launcher icon is the grey artwork in src/debug/res, which overrides src/main by resource merging.
      // Pointing the manifest at a differently named resource instead would hide both icon sets from lint, since it only ever analyses one variant.
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "r8-rules.pro")
    }
    release {
      isMinifyEnabled = true
      // Safe here because nothing resolves a resource by name at runtime - there is no Resources.getIdentifier call anywhere in the tree.
      isShrinkResources = true
      // Absent when no keystore is configured; the APK then builds unsigned.
      signingConfig = signingConfigs.findByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "r8-rules.pro")
    }
  }

  lint {
    checkReleaseBuilds = false
  }

  // https://github.com/trakt/showly/issues/46#issuecomment-2324619155
  dependenciesInfo {
    // Disables dependency metadata when building APKs.
    includeInApk = false
    // Disables dependency metadata when building Android App Bundles.
    includeInBundle = false
  }

  namespace = "xyz.stignarnia.shelfly"
}

// The Hilt extension belongs to the project rather than to android {}, where Groovy's delegation happened to find it.
configure<dagger.hilt.android.plugin.HiltExtension> {
  enableExperimentalClasspathAggregation = true
}

// Stamps every debug build so consecutive ones can be told apart on the device.
// Without it they all report the same version and there is no way to see which build is installed.
//
// The stamp increases rather than being random: versionCode is an int and Android refuses to install a lower one over a higher one, so a random value would fail as soon as it landed below the installed build.
// Seconds since the epoch is monotonic and stays inside Integer.MAX until 2038.
//
// Read through a ValueSource, not directly: with the configuration cache on, a value computed while configuring is stored in the cache entry and replayed, which would hand every later build the same stamp.
abstract class BuildStamp : ValueSource<Long, ValueSourceParameters.None> {
  override fun obtain(): Long = System.currentTimeMillis() / 1000L
}

extensions.configure<com.android.build.api.variant.ApplicationAndroidComponentsExtension>("androidComponents") {
  onVariants(selector().withBuildType("debug")) { variant ->
    val releaseName = libs.versions.versionName.get()
    val stamp = providers.of(BuildStamp::class.java) {}
    variant.outputs.forEach { output ->
      output.versionCode.set(stamp.map { it.toInt() })
      output.versionName.set(stamp.map { "$releaseName-debug-$it" })
    }
  }
}

dependencies {
  implementation(project(":common"))
  implementation(project(":data-remote"))
  implementation(project(":data-local"))
  implementation(project(":repository"))
  implementation(project(":ui-base"))
  implementation(project(":ui-backup"))
  implementation(project(":ui-model"))
  implementation(project(":ui-navigation"))
  implementation(project(":ui-discover"))
  implementation(project(":ui-discover-movies"))
  implementation(project(":ui-episodes"))
  implementation(project(":ui-lists"))
  implementation(project(":ui-show"))
  implementation(project(":ui-movie"))
  implementation(project(":ui-gallery"))
  implementation(project(":ui-my-shows"))
  implementation(project(":ui-my-movies"))
  implementation(project(":ui-search"))
  implementation(project(":ui-statistics"))
  implementation(project(":ui-statistics-movies"))
  implementation(project(":ui-settings"))
  implementation(project(":ui-progress"))
  implementation(project(":ui-progress-movies"))
  implementation(project(":ui-widgets"))

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.work)
  ksp(libs.hilt.work.compiler)

  testImplementation(libs.bundles.testing)
  androidTestImplementation(libs.android.test.runner)

  coreLibraryDesugaring(libs.android.desugar)
}
