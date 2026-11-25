plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("org.jetbrains.compose") version "1.9.3"
    application
}
kotlin {
    jvmToolchain(17)
    sourceSets.main {
        kotlin.srcDir("../../maduikit-components/src/commonMain/kotlin")
        kotlin.srcDir("../../maduikit-theme/src/commonMain/kotlin")
    }
}
dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
application {
    mainClass.set("LengthProbeKt")
    applicationDefaultJvmArgs = listOf("-Djava.awt.headless=true") +
        providers.gradleProperty("smokeUserHome").orNull?.let { listOf("-Duser.home=$it") }.orEmpty()
}
