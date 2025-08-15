// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1")
        classpath("com.google.gms:google-services:4.4.2")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
    }
}