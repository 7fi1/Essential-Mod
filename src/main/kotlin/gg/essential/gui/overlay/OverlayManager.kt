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
package gg.essential.gui.overlay

/**
 * Manages [Layer]s to be displayed above the vanilla screen.
 */
interface OverlayManager {
    /**
     * Creates a new [PersistentLayer] with the given priority (above existing layers with the same priority).
     */
    fun createPersistentLayer(priority: LayerPriority): PersistentLayer

    /**
     * Creates a new [EphemeralLayer] with the given priority (above existing layers with the same priority).
     */
    fun createEphemeralLayer(priority: LayerPriority): EphemeralLayer

    /**
     * Forcefully removes the given layer.
     *
     * This will not call [EphemeralLayer.onClose] or any other cleanup methods.
     */
    fun removeLayer(layer: Layer)
}