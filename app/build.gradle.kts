import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "app.shutterup"
    compileSdk = 36

    defaultConfig {
        // Stable from day one: changing it later breaks Auto Backup restore (SPEC §16).
        applicationId = "app.shutterup"
        minSdk = 34
        targetSdk = 36
        // Every CI APK is installable over the previous one (SPEC §16.1).
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release signing comes from environment variables populated by the
        // build-apk workflow (SPEC §16.1). Absent locally/forks: fall back to
        // the debug key so every build still succeeds.
        val keystorePath = System.getenv("SHUTTERUP_KEYSTORE_PATH")
        val keystorePassword = System.getenv("SHUTTERUP_KEYSTORE_PASSWORD")
        val keyAlias = System.getenv("SHUTTERUP_KEY_ALIAS")
        val keyPassword = System.getenv("SHUTTERUP_KEY_PASSWORD")
        if (!keystorePath.isNullOrBlank() && !keystorePassword.isNullOrBlank()
            && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            // No applicationIdSuffix: the debug build stands in for the app on
            // the Fold 7 during device verification.
        }
        release {
            isMinifyEnabled = false
            // Uses the "release" signing config when secrets exist, debug otherwise.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            if (signingConfigs.findByName("release") == null) {
                println("WARNING: release keystore secrets absent; signing release with debug key (SPEC §16.1).")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Release gate: Android Lint is fatal on release (SPEC §12).
        abortOnError = true
        checkReleaseBuilds = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Kotlin compilation is built into AGP 9; configure the toolchain target on the
// compile tasks directly.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

// Room schema export for reviewed migrations (SPEC §10).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose (versions via BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Adaptive layouts
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.androidx.navigation.suite)

    // Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Background work
    implementation(libs.androidx.work.runtime)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // On-device AI (Gemini Nano via ML Kit GenAI Prompt API; SPEC §7.2)
    implementation(libs.mlkit.genai.prompt)
    ksp(libs.mlkit.genai.schema.compiler)

    // Images
    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)

    // Widget
    implementation(libs.glance.appwidget)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Unit tests
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    // Robolectric runs SDK-35 shadows (JDK 17 compatible); DAO behavior is SDK-independent.
    testImplementation(libs.robolectric)
    // Screenshot tests (Roborazzi; record with recordRoborazziDebug, verified in CI)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

roborazzi {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

// The permission guard test reads the merged manifest (SPEC §15.1 ManifestGuardTest).
// Looked up after evaluation: AGP realizes test tasks late.
afterEvaluate {
    tasks.named("testDebugUnitTest") {
        dependsOn("processDebugMainManifest")
    }
}

// Anchor the guard test to absolute paths: unit-test working dirs vary.
tasks.withType<Test>().configureEach {
    systemProperty(
        "shutterup.mergedManifest",
        file("$buildDir/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml").absolutePath,
    )
    systemProperty(
        "shutterup.sourceManifest",
        file("$projectDir/src/main/AndroidManifest.xml").absolutePath,
    )
}
