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
package gg.essential.util.image

import gg.essential.model.util.Color
import gg.essential.universal.render.UGpuTexture
import gg.essential.universal.render.UGpuTextureView
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.image.bitmap.Bitmap
import java.nio.FloatBuffer

interface GpuTexture : AutoCloseable {
    val uc: UGpuTexture
    val ucView: UGpuTextureView

    val glId: Int
    val width: Int
    val height: Int

    override fun close() // does not throw Exception

    data class CopyOp(val src: GpuTexture, val srcX: Int, val srcY: Int, val destX: Int, val destY: Int, val width: Int, val height: Int)
    fun copyFrom(sources: Iterable<CopyOp>)

    fun copyFrom(source: GpuTexture) {
        if (width != source.width || height != source.height) {
            throw IllegalArgumentException("Source size (${source.width}x${source.height}) does not match target size (${width}x${height})")
        }
        copyFrom(listOf(CopyOp(source, 0, 0, 0, 0, width, height)))
    }

    fun clearColor(color: Color = Color(0u))
    fun clearDepth(depth: Float = if (platform.usesReversedZ) 0f else 1f)

    fun readPixelColor(x: Int, y: Int): Color = readPixelColors(x, y, 1, 1)[0, 0]
    fun readPixelDepth(x: Int, y: Int): Float = readPixelDepths(x, y, 1, 1).get(0)

    fun readPixelColors(x: Int, y: Int, width: Int, height: Int): Bitmap
    fun readPixelDepths(x: Int, y: Int, width: Int, height: Int): FloatBuffer

    enum class Format(val isColor: Boolean) {
        RGBA8(true),
        DEPTH24_STENCIL8(false),
        DEPTH32(false),
    }

    companion object {
        operator fun invoke(width: Int, height: Int, format: Format): GpuTexture = platform.newGpuTexture(width, height, format)
    }
}
