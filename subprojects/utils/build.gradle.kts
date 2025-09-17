/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
import gg.essential.gradle.util.KotlinVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `java-library`
}

repositories {
    mavenCentral()
}

kotlin {
    // Common dependencies
    dependencies {
        val kotlin = KotlinVersion.minimal
        implementation(kotlin("stdlib", kotlin.stdlib))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlin.coroutines}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:${kotlin.serialization}")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${kotlin.serialization}")
        api("dev.folomeev.kotgl:kotgl-matrix:0.0.1-beta")
    }

    // JVM dependencies
    dependencies {
        api(libs.slf4j.api)
    }

    kotlin.jvmToolchain(8)
}

// Using src dirs matching what kotlin-multiplatform would do because we ideally want to be using that,
// but have moved away from it for the time being because it was too unreliable.
sourceSets.main {
    java.srcDir("src/commonMain/java")
    java.srcDir("src/jvmMain/java")
    kotlin.srcDir("src/commonMain/kotlin")
    kotlin.srcDir("src/jvmMain/kotlin")
}
