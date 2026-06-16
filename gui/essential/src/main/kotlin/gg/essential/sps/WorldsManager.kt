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
@file:UseSerializers(UuidAsStringSerializer::class, InstantAsMillisSerializer::class)

package gg.essential.sps

import gg.essential.model.util.InstantAsMillisSerializer
import gg.essential.network.connectionmanager.common.model.ModLoaderType
import gg.essential.util.UuidAsStringSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.*
import kotlin.io.path.div
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

private val leanJson = Json { ignoreUnknownKeys = true }

@Serializable
enum class GameModLoader(
    val infraModLoader: ModLoaderType
) {
    Fabric(ModLoaderType.FABRIC),
    NeoForge(ModLoaderType.NEOFORGE),
    Forge(ModLoaderType.FORGE),
    ;
}

fun ModLoaderType.toMod(): GameModLoader {
    return GameModLoader.entries.first { it.infraModLoader == this }
}

private operator fun Instant.minus(other: Instant): Duration =
    java.time.Duration.between(other, this).toKotlinDuration()
