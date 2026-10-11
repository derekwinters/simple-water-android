import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// APP-004: plain Kotlin/JVM. Everything here is free of Android types, so `./gradlew :core:test`
// needs no Android SDK. The app depends on it for goal maths, units, day windows, reminder slots
// and history summaries.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
    testImplementation(libs.junit)
}
