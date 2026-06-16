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
package gg.essential.gui.notification

import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.Slot
import gg.essential.elementa.components.UIContainer
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.elementa.state.v2.flatten
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignVertical
import gg.essential.gui.layoutdsl.box
import gg.essential.gui.layoutdsl.childBasedHeight
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillRemainingWidth
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.height
import gg.essential.gui.layoutdsl.heightAspect
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.hoverTooltip
import gg.essential.gui.layoutdsl.image
import gg.essential.gui.layoutdsl.layoutAsColumn
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.whenTrue
import gg.essential.gui.layoutdsl.width
import gg.essential.sps.SpsAddress
import gg.essential.util.CachedAvatarImage
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.ServerPingInfo
import gg.essential.util.UuidNameLookup
import gg.essential.util.colored
import gg.essential.util.loadUIImage
import gg.essential.util.thenAcceptOnMainThread
import gg.essential.util.toImageFactory
import kotlinx.coroutines.launch
import java.awt.Color
import java.util.UUID

fun sendTosNotification(viewButtonAction: () -> Unit) {
    Notifications.pushPersistentToast(
        "Terms of Service",
        "This feature requires you to accept the Essential ToS.",
        action = {},
        close = {},
    ) {
        uniqueId = object {}.javaClass
        withCustomComponent(Slot.ICON, EssentialPalette.ROUND_WARNING_7X.create())
        withCustomComponent(Slot.ACTION, toastButton("View", action = viewButtonAction))
    }
}

fun sendCheckoutFailedNotification() {
    Notifications.pushPersistentToast(
        "Error",
        "An issue occurred while trying to send you to checkout. Please try again later.",
        {},
        {},
    ) {
        type = NotificationType.ERROR
    }
}

fun sendSpsInviteNotification(uuid: UUID) =
    UuidNameLookup.getName(uuid).thenAcceptOnMainThread { name ->
        sendSpsInviteNotification(uuid, name)
    }

fun sendServerInviteNotification(uuid: UUID, address: String, onJoining: () -> Unit = {}) =
    UuidNameLookup.getName(uuid).thenAcceptOnMainThread { name ->
        sendServerInviteNotification(uuid, name, address, onJoining)
    }

fun sendSpsInviteNotification(uuid: UUID, name: String) {
    val address = SpsAddress(uuid).toString()
    sendInviteNotification(uuid, name, address, true)
}

fun sendServerInviteNotification(uuid: UUID, name: String, address: String, onJoining: () -> Unit = {}) =
    sendInviteNotification(uuid, name, address, false, onJoining)

private fun sendInviteNotification(uuid: UUID, name: String, address: String, isSps: Boolean, onJoining: () -> Unit = {}) {
    platform.cmConnection.connectionScope.launch {
        val serverInfo = ServerPingInfo.fetchViaPingProxy(address)
        val icon = serverInfo?.iconImage?.let { loadUIImage(it).toImageFactory() } ?: EssentialPalette.PACK_COLOR_128X
        Notifications.pushPersistentToast(name, "", {}, {}) {
            uniqueId = if (isSps) SPSNotificationId(uuid) else null

            withCustomComponent(Slot.ICON, CachedAvatarImage.create(uuid))

            val content = UIContainer()
            content.layoutAsColumn(Modifier.fillWidth().childBasedHeight(), Arrangement.spacedBy(6f)) {
                box(Modifier.fillWidth().height(1f).color(EssentialPalette.COMPONENT_BACKGROUND_HIGHLIGHT)) // Top line
                row(Modifier.fillWidth(), Arrangement.spacedBy(8f)) {
                    image(icon, Modifier.width(19f).heightAspect(1f).shadow(Color.BLACK))
                    column(Modifier.fillRemainingWidth().alignVertical(Alignment.Start), Arrangement.spacedBy(4f), Alignment.Start) {
                        val serverName = if (isSps) {
                            val formattedUsername = if (name.isNotBlank()) "$name's " else ""
                            serverInfo?.worldName ?: "${formattedUsername}World"
                        } else {
                            address
                        }
                        val truncatedState = mutableStateOf(stateOf(false))
                        val nameTooltipModifier = Modifier.whenTrue(
                            truncatedState.flatten(),
                            Modifier.hoverTooltip(serverName, position = EssentialTooltip.Position.MOUSE)
                        ).hoverScope()
                        text(
                            serverName,
                            nameTooltipModifier.color(EssentialPalette.TEXT).shadow(Color.BLACK),
                            truncateIfTooSmall = true,
                            showTooltipForTruncatedText = false,
                        ).also {
                            truncatedState.set(it.truncatedState)
                        }
                        if (serverInfo != null) {
                            val serverDetails = if (isSps) {
                                "${serverInfo.onlinePlayers}/${serverInfo.maxPlayers} Players"
                            } else {
                                val onlinePlayers = "%,d".format(serverInfo.onlinePlayers)
                                "$onlinePlayers Player${if (serverInfo.onlinePlayers != 1) "s" else ""}"
                            }
                            text(serverDetails, Modifier.color(EssentialPalette.TEXT).shadow(Color.BLACK))
                        }
                    }
                    toastButton(
                        "Join",
                        backgroundModifier = Modifier.color(EssentialPalette.BLUE_BUTTON).hoverColor(EssentialPalette.BLUE_BUTTON_HOVER).shadow(Color.BLACK),
                        textModifier = Modifier.color(EssentialPalette.TEXT_HIGHLIGHT).shadow(EssentialPalette.TEXT_SHADOW)
                    ) {
                        dismissNotification()
                        platform.connectToServer(name, address)
                        onJoining()
                    }(Modifier.alignVertical(Alignment.End(1f)))
                }
            }
            withCustomComponent(Slot.LARGE_PREVIEW, content)
        }
    }
}

fun sendOutgoingSpsInviteNotification(name: String) {
    Notifications.push("", "") {
        iconAndMarkdownBody(EssentialPalette.ENVELOPE_9X7.create(), "${name.colored(EssentialPalette.TEXT_HIGHLIGHT)} invited")
    }
}

data class SPSNotificationId(val uuid: UUID)