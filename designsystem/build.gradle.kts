import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// :designsystem is an Android library holding the Compose token layer and component vocabulary,
// ported from Interval-trainer-android (docs/spec/design-system.md). material3 is declared
// `implementation`, never `api`, so :app has no compile-time access to any Material 3 type: a raw
// Material component is reachable only from inside this module (DS-090).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.derekwinters.water.designsystem"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Icon data only (ArrowBack, Add, Settings, ...), not a Material component (DS-090).
    implementation(libs.androidx.compose.material.icons.core)
    // DS-090: implementation, never api.
    implementation(libs.androidx.compose.material3)

    // ColorSchemeMappingTest runs on the JVM alone: Color and ColorScheme are plain types.
    testImplementation(libs.junit)
}
