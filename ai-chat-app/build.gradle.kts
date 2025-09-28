// Top‑level build configuration for the AI Chat App.
// This file configures repositories and plugins that are common to all modules.

plugins {
    // The Android plugin provides tasks for building and packaging the app.
    id("com.android.application") version "8.0.0" apply false
    // Kotlin Android plugin adds support for Kotlin language features on Android.
    kotlin("android") version "1.9.10" apply false
    kotlin("kapt") version "1.9.10" apply false
}

// Define repositories used by all modules in this project.
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}