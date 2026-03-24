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
package gg.essential.gui.common.modal

import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.SequenceAnimatedUIImage
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.overlay.ModalFlow
import gg.essential.gui.overlay.ModalManager
import java.awt.Color
import java.util.concurrent.TimeUnit

class ConnectingModal(
    modalManager: ModalManager,
    private val title: String,
    private val isConnecting: State<Boolean>,
    private val continuation: ModalFlow.ModalContinuation<Boolean>
) : EssentialModal2(modalManager) {

    private var unregisterEffect: (() -> Unit)? = null
    private var hasResumed = false

    override fun onOpen() {
        super.onOpen()
        // Immediately move on if connected
        unregisterEffect = effect(this) {
            if (!isConnecting()) {
                hasResumed = true
                replaceWith(continuation.resumeImmediately(true))
            }
        }
    }

    override fun onClose() {
        super.onClose()
        unregisterEffect?.invoke()
        if (!hasResumed) {
            modalManager.queueModal(continuation.resumeImmediately(false))
        }
    }

    override fun LayoutScope.layoutTitle() {
        title(title)
    }

    override fun LayoutScope.layoutBody() {
        box(Modifier.childBasedHeight(6f)) {
            SequenceAnimatedUIImage(
                "/assets/essential/textures/loading/loading_", ".png",
                12,
                80,
                TimeUnit.MILLISECONDS,
            )(Modifier.color(EssentialPalette.TEXT).shadow(Color.BLACK))
        }
    }

    override fun LayoutScope.layoutButtons() {
        cancelButton("Cancel") {
            hasResumed = true
            replaceWith(continuation.resumeImmediately(false))
        }
    }
}