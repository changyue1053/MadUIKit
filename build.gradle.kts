plugins {
    kotlin("multiplatform") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.compose") version "1.9.3" apply false
    id("com.android.library") version "8.11.1" apply false
}

allprojects {
    group = "com.mad.maduikit"
    version = "0.1.0-alpha21-sdk-preview01"
}
