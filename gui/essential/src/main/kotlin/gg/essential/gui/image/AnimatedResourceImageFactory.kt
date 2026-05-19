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
package gg.essential.gui.image

import gg.essential.config.LoadsResources
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.gui.common.SequenceAnimatedUIImage
import java.util.concurrent.TimeUnit

class AnimatedResourceImageFactory @LoadsResources("%pathPrefix%[0-9]+%pathSuffix%") constructor(
    pathPrefix: String,
    pathSuffix: String,
    frameCount: Int,
    private val msPerFrame: Long,
    preload: Boolean = true
) {
    private val frames = List(frameCount, init = { i ->
        ResourceImageFactory("$pathPrefix${i + 1}$pathSuffix", preload)
    })

    fun create(): SequenceAnimatedUIImage {
        return SequenceAnimatedUIImage(frames, msPerFrame, TimeUnit.MILLISECONDS)
    }

    companion object {
        init {
            Inspector.registerComponentFactory(AnimatedResourceImageFactory::class.java)
        }
    }
}
