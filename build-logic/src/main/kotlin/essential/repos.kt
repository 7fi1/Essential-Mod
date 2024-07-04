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
package essential

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.*

// Using our mirror because JitPack itself is very unreliable from time to time
fun RepositoryHandler.jitpack() = maven("https://repo.essential.gg/repository/maven-public")

fun RepositoryHandler.minecraft() = maven("https://libraries.minecraft.net")

fun RepositoryHandler.mixin() = maven("https://repo.spongepowered.org/repository/maven-releases/")

fun RepositoryHandler.modMenu() = maven("https://maven.terraformersmc.com/releases/") {
    content {
        includeGroup("com.terraformersmc")
    }
}

// Documentation: https://docs.modrinth.com/maven
fun RepositoryHandler.modrinth() = maven("https://api.modrinth.com/maven") {
    content {
        includeGroup("maven.modrinth")
    }
}
