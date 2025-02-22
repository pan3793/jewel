plugins {
    id("org.jetbrains.jewel.kotlin")
    alias(libs.plugins.composeDesktop)
    alias(libs.plugins.ideaGradlePlugin)
    id("org.jetbrains.jewel.detekt")
    id("org.jetbrains.jewel.ktlint")
}

intellij {
    pluginName.set("Jewel")
    version.set("LATEST-EAP-SNAPSHOT")
    plugins.set(listOf("org.jetbrains.kotlin"))
    version.set("2022.3")
}

// TODO remove this once the IJ Gradle plugin fixes their repositories bug
// See https://github.com/JetBrains/gradle-intellij-plugin/issues/776
repositories {
    maven("https://androidx.dev/storage/compose-compiler/repository/")
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    mavenCentral()
}

dependencies {
    implementation(projects.themes.darcula.darculaIde)
}
