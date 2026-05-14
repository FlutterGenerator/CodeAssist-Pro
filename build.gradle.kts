// Top-level build file where you can add configuration options common to all sub-projects/modules.
@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.plugins.JavaPluginExtension

plugins{

}

buildscript {
    repositories {
        google()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://mvnrepository.com/") }
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
        mavenCentral()
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases/") }
        maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/") }
    }

    dependencies {
        classpath(libs.android.gradle.plugin)
        classpath(libs.kotlin.gradle.plugin)
        classpath("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
        classpath(libs.kotlin.serialization.plugin)
        classpath(libs.nav.safe.args.gradle.plugin)
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

extra.apply {
    set("compileSdkVersion", 36)
    set("buildToolsVersion", "36.1.0")
    set("targetSdkVersion", 36)
    set("minSdkVersion", 26)
    set("applicationId", "dev.mutwakil.codeassist")
    set("versionCode", 1)
    set("versionName", "0.2.0-ALPHA02")
}
