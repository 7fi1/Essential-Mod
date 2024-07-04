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
package gg.essential.gui.screenshot.editor.change

import gg.essential.gui.screenshot.editor.ScreenshotCanvas
import gg.essential.util.lwjgl3.api.nanovg.NanoVG

/**
 * Vector stroke
 */
abstract class VectorStroke(val editableScreenshot: ScreenshotCanvas, val color: Int) : Change {

    /**
     * Renders this VectorStroke
     */
    abstract fun render(vg: NanoVG, width: Float, height: Float, scale: Float)

    override fun undo(canvas: ScreenshotCanvas) {
        canvas.vectorEditingOverlay.markDirty()
    }

    override fun redo(canvas: ScreenshotCanvas) {
        canvas.vectorEditingOverlay.markDirty()
    }
}