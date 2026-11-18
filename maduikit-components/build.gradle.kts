import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.library")
    `maven-publish`
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        publishLibraryVariants("release")
    }
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            api(project(":maduikit-theme"))
            api(compose.runtime)
            api(compose.ui)
            api(compose.foundation) // Public row/box slots expose Foundation layout scopes.
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
        }
    }
}

android {
    namespace = "com.mad.maduikit.components"
    compileSdk = 36
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    repositories {
        maven {
            // Filesystem-only release rehearsal; no remote endpoint or credentials.
            name = "madUIKitLocal"
            url = rootProject.file(
                providers.gradleProperty("maduikitStagingDir").getOrElse("../maduikit-maven")
            ).toURI()
        }
    }
}
