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
package gg.essential.gradle.util

import gg.essential.gradle.multiversion.Platform

data class KotlinVersion(
    /** Version of fabric-language-kotlin or KotlinForForge */
    val mod: String?,
    val stdlib: String,
    val coroutines: String,
    val serialization: String,
) {
    companion object {
        val latest = KotlinVersion(null, "1.9.23", "1.8.0", "1.6.3")

        val fabricLanguageKotlin = latest.copy(mod = "1.10.19")
        val kotlinForForge1 = latest.copy(mod = "1.17.0")
        val kotlinForForge2 = latest.copy(mod = "2.2.0")
        val kotlinForForge3 = latest.copy(mod = "3.6.0")
        val kotlinForForge4 = latest.copy(mod = "4.3.0")

        val minimal = latest
    }
}

val Platform.kotlinVersion
    get() =
        when {
            isModLauncher ->
                when (mcVersion) {
                    12105, 12104, 12103, 12101, 12006, 12004, 12002, 12001, 11904, 11903 -> KotlinVersion.kotlinForForge4
                    11902, 11802 -> KotlinVersion.kotlinForForge3
                    11701 -> KotlinVersion.kotlinForForge2
                    11602 -> KotlinVersion.kotlinForForge1
                    else -> throw UnsupportedOperationException("Missing Kotlin version for $this")
                }
            isFabric -> KotlinVersion.fabricLanguageKotlin
            else -> KotlinVersion.latest
        }
