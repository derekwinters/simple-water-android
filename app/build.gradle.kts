import com.android.build.api.variant.impl.VariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

// APP-005: gradle.properties is the single source of truth for the app's version. VERSION_NAME
// carries a trailing `# x-release-please-version` marker that release-please's generic updater
// rewrites; Java properties do not treat an inline `#` as a comment, so it is stripped here.
val versionNameProperty = (project.findProperty("VERSION_NAME") as String)
    .substringBefore("#")
    .trim()
val versionCodeProperty = (project.findProperty("VERSION_CODE") as String)
    .substringBefore("#")
    .trim()
    .toInt()

// APP-006: release signing comes from environment variables only, never a committed file or a
// Gradle property (docs/adr/0001). ANDROID_KEYSTORE_PATH is the path of the keystore a workflow
// decodes from the ANDROID_KEYSTORE_BASE64 secret; the other three are secrets of the same names.
// When any of the four is missing (a local build, a fork PR, a mis-wired workflow) no `release`
// signing config is created and the release build type is left UNSIGNED, never debug-signed.
val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyAliasPassword = System.getenv("ANDROID_KEY_ALIAS_PASSWORD")
val hasReleaseSigningConfig = !releaseKeystorePath.isNullOrBlank() &&
    !releaseKeystorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyAliasPassword.isNullOrBlank()

android {
    namespace = "com.derekwinters.water"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.derekwinters.water"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeProperty
        versionName = versionNameProperty
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyAliasPassword

                // Every signature scheme stated rather than defaulted, as in the sibling repos.
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // LOG-012: the Room migration test runs on the JVM under Robolectric.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Room exports its schema here so schema changes are reviewable in PRs. Commit app/schemas/.
room {
    schemaDirectory("$projectDir/schemas")
}

// APP-005: every variant's APK is named `simple-water-<versionName>-<buildType>.apk`, so
// workflows can glob app/build/outputs/apk/<buildType>/*.apk. outputFileName lives only on the
// internal VariantOutputImpl, hence the cast (same approach as the sibling Android repos).
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is VariantOutputImpl) {
                output.outputFileName.set("simple-water-$versionNameProperty-${variant.buildType}.apk")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true
    }
}

dependencies {
    implementation(project(":core"))
    // DS-090: the design system exposes its components; material3 stays inside it.
    implementation(project(":designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    // UI-014–019: the home screen widget.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
