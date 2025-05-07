// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "2.9.9" apply false  // 添加 Crashlytics 插件
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4")
        classpath("com.google.gms:google-services:4.4.2")  // Google Services 插件
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")  // Crashlytics 插件
    }
}