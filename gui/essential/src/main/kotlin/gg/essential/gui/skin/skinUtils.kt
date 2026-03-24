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
package gg.essential.gui.skin

import com.sparkuniverse.toolbox.chat.model.Channel
import gg.essential.api.gui.Slot
import gg.essential.connectionmanager.common.packet.skin.ClientSkinSharePacket
import gg.essential.elementa.components.UIContainer
import gg.essential.gui.EssentialPalette
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.modals.select.SelectModal
import gg.essential.gui.modals.select.friendsAndGroups
import gg.essential.gui.modals.select.selectModal
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.content.SkinPreviewToastComponent
import gg.essential.gui.notification.error
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.wardrobe.Item
import gg.essential.mod.Skin
import gg.essential.model.util.Color
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.image.bitmap.Bitmap
import kotlinx.coroutines.launch
import java.util.*

fun createSkinShareModal(
    modalManager: ModalManager,
    skin: Item.SkinItem,
    onModalCancelled: (Boolean) -> Unit = {},
    onComplete: () -> Unit = {}
): SelectModal<Channel> {
    return selectModal(modalManager, "Share Skin", "ShareSkin") {
        friendsAndGroups()

        modalSettings {
            primaryButtonText = "Share"
            cancelButtonText = "Cancel"
            onCancel(onModalCancelled)
        }

        selectTooltip = "Add"
        deselectTooltip = "Remove"

        requiresSelection = true
        requiresButtonPress = false
    }.onPrimaryAction { selectedChannels ->
        platform.cmConnection.connectionScope.launch {
            if (!platform.cmConnection.call(ClientSkinSharePacket(selectedChannels.map { it.id }.toSet(), skin.id)).awaitResponseActionPacket()) {
                Notifications.error("Failed to share skin", "")
            }
        }
        onComplete()
    }
}

fun showSkinReceivedToast(skin: Skin, uuid: UUID, username: String, channel: Channel) {
    Notifications.push("", "", 4f, {
        platform.openSocialMenu(channel.id)
    }) {
        withCustomComponent(Slot.SMALL_PREVIEW, SkinPreviewToastComponent(skin))
        withCustomComponent(Slot.PREVIEW, SkinReceivedNotificationComponent(uuid, username))
    }
}

private class SkinReceivedNotificationComponent(uuid: UUID, username: String) : UIContainer() {
    init {
        layout(Modifier.childBasedWidth(1f).childBasedHeight()) {
            column(Modifier.alignHorizontal(Alignment.End), horizontalAlignment = Alignment.Start) {
                spacer(height = 2f)
                row(Modifier.childBasedMaxHeight(), verticalAlignment = Alignment.End) {
                    spacer(width = 1f)
                    CachedAvatarImage.create(uuid)(Modifier.alignVertical(Alignment.Start).shadow(EssentialPalette.TEXT_SHADOW_LIGHT))
                    spacer(width = 5f)
                    text(username)
                }
                spacer(height = 7f)
                row(Arrangement.spacedBy(5f), Alignment.Start) {
                    image(EssentialPalette.PERSON_4X6, Modifier.shadow(EssentialPalette.TEXT_SHADOW_LIGHT))
                    text("Shared a skin.")
                }
                spacer(height = 1f)
            }
        }
    }
}

fun preprocessSkinImage(image: Bitmap): Bitmap {

    val newImage = Bitmap.ofSize(64, 64)
    // Should always be 64 or smaller (64x64 or 64x32)
    newImage.set(0, 0, image.width, image.height, image)

    fun setAreaOpaque(x1: Int, y1: Int, x2: Int, y2: Int) {
        for (y in y1 until y2) {
            for (x in x1 until x2) {
                newImage[x, y] = newImage[x, y].copy(a = 255U)
            }
        }
    }

    fun setAreaTransparent(x1: Int, y1: Int, x2: Int, y2: Int) {
        for (y in y1 until y2) {
            for (x in x1 until x2) {
                if (newImage[x, y].a < 128U) {
                    return
                }
            }
        }

        for (y in y1 until y2) {
            for (x in x1 until x2) {
                newImage[x, y] = newImage[x, y].copy(a = 0U)
            }
        }
    }

    val isOldSkinHeight = image.height == 32
    if (isOldSkinHeight) {
        newImage[0, 32, 64, 32] = Color(0U)
        newImage.set(20, 48, 4, 4, image, 4, 16, mirrorX = true)
        newImage.set(24, 48, 4, 4, image, 8, 16, mirrorX = true)
        newImage.set(24, 52, 4, 12, image, 0, 20, mirrorX = true)
        newImage.set(20, 52, 4, 12, image, 4, 20, mirrorX = true)
        newImage.set(16, 52, 4, 12, image, 8, 20, mirrorX = true)
        newImage.set(28, 52, 4, 12, image, 12, 20, mirrorX = true)
        newImage.set(36, 48, 4, 4, image, 44, 16, mirrorX = true)
        newImage.set(40, 48, 4, 4, image, 48, 16, mirrorX = true)
        newImage.set(40, 52, 4, 12, image, 40, 20, mirrorX = true)
        newImage.set(36, 52, 4, 12, image, 44, 20, mirrorX = true)
        newImage.set(32, 52, 4, 12, image, 48, 20, mirrorX = true)
        newImage.set(44, 52, 4, 12, image, 52, 20, mirrorX = true)
    }
    setAreaOpaque(0, 0, 32, 16)
    if (isOldSkinHeight) {
        setAreaTransparent(32, 0, 64, 32)
    }
    setAreaOpaque(0, 16, 64, 32)
    setAreaOpaque(16, 48, 48, 64)
    return newImage
}
