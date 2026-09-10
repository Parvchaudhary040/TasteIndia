// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP 9 ships built-in Kotlin and forbids applying `org.jetbrains.kotlin.android`. Its default
// KGP is 2.2.10, which is too old for this dependency set (Coil 3.6 / Feb-2026 Compose BOM pull
// kotlin-stdlib 2.4.10). The documented way to bump built-in Kotlin is to put a newer KGP (and a
// matching KSP) on the buildscript classpath. See https://kotl.in/gradle/agp-built-in-kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}
