import java.io.ByteArrayOutputStream
import javax.inject.Inject

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.mybrowser.bmcore"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

/**
 * Cross-compiles the Rust core (`rust/bmcore`) for each Android ABI with cargo-ndk into a
 * jniLibs-shaped directory that AGP packages as `lib/<abi>/libbmcore.so`.
 *
 * Without cargo-ndk the task only warns and the app runs on the pure-Kotlin fallback
 * (`KotlinCoreAlgorithms`); pass `-Pbmcore.requireRust=true` (as CI does) to make it mandatory.
 */
abstract class CargoNdkBuildTask : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val rustSources: ConfigurableFileCollection

  @get:Internal abstract val crateDir: DirectoryProperty

  @get:Input abstract val abis: ListProperty<String>

  @get:Input abstract val platformApi: Property<Int>

  @get:Input abstract val requireRust: Property<Boolean>

  @get:OutputDirectory abstract val outputDir: DirectoryProperty

  @get:Inject abstract val execOperations: ExecOperations

  init {
    // A run without cargo-ndk leaves an empty directory; never treat that as up to date, so the
    // library gets built as soon as the toolchain becomes available.
    outputs.upToDateWhen {
      outputDir.get().asFile.walkTopDown().any { it.isFile && it.name.endsWith(".so") }
    }
  }

  @TaskAction
  fun build() {
    val out = outputDir.get().asFile
    out.deleteRecursively()
    out.mkdirs()

    val probeExit = runCatching {
      execOperations.exec {
        commandLine("cargo", "ndk", "--version")
        isIgnoreExitValue = true
        standardOutput = ByteArrayOutputStream()
        errorOutput = ByteArrayOutputStream()
      }.exitValue
    }.getOrDefault(-1)
    if (probeExit != 0) {
      val message =
        "cargo-ndk not found: libbmcore.so will not be bundled and the pure-Kotlin core fallback is used."
      if (requireRust.get()) throw GradleException(message)
      logger.warn("w: $message")
      return
    }

    val args = buildList {
      add("cargo")
      add("ndk")
      abis.get().forEach { abi ->
        add("-t")
        add(abi)
      }
      add("--platform")
      add(platformApi.get().toString())
      add("-o")
      add(out.absolutePath)
      add("build")
      add("--release")
      add("--locked")
    }
    execOperations.exec {
      workingDir = crateDir.get().asFile
      commandLine(args)
    }
  }
}

val cargoNdkBuild = tasks.register<CargoNdkBuildTask>("cargoNdkBuild") {
  val crate = rootProject.layout.projectDirectory.dir("rust/bmcore")
  crateDir.set(crate)
  rustSources.from(crate.dir("src"), crate.file("Cargo.toml"), crate.file("Cargo.lock"))
  abis.set(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
  platformApi.set(24) // keep in sync with defaultConfig.minSdk
  requireRust.set(
    providers.gradleProperty("bmcore.requireRust").map { it.toBoolean() }.orElse(false)
  )
  outputDir.set(layout.buildDirectory.dir("generated/rustJniLibs"))
}

androidComponents {
  onVariants { variant ->
    variant.sources.jniLibs?.addGeneratedSourceDirectory(cargoNdkBuild, CargoNdkBuildTask::outputDir)
  }
}

// Pure AOSP / Zero-GMS dependencies only.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.webkit)
  implementation(libs.androidx.window)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.serialization.json)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
