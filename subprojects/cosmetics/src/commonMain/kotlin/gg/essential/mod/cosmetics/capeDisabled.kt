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
package gg.essential.mod.cosmetics

import gg.essential.mod.EssentialAsset
import gg.essential.model.util.now
import gg.essential.network.cosmetics.Cosmetic

const val CAPE_DISABLED_COSMETIC_ID = "CAPE_DISABLED"

@JvmField
val CAPE_DISABLED_COSMETIC = Cosmetic(
    CAPE_DISABLED_COSMETIC_ID,
    CosmeticType("CAPE", CosmeticSlot.CAPE, emptyMap(), emptyMap()),
    CosmeticTier.COMMON,
    emptyMap(),
    mapOf("geometry.steve.json" to EssentialAsset.of("""{"format_version": "1.12.0"}""")),
    emptyList(),
    -1,
    emptyMap(),
    emptySet(),
    now(),
    null,
    null,
    null,
    emptyMap(),
    emptyMap(),
    0,
)
