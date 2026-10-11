// Plugin versions live in gradle/libs.versions.toml and are applied in the modules (APP-003).
// AGP 8.7.3 / Kotlin 2.0.21 match Interval-trainer-android, whose design system is ported here.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
}
