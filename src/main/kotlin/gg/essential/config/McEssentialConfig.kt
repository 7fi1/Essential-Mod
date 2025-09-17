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
package gg.essential.config

import com.sparkuniverse.toolbox.relationships.enums.FriendRequestPrivacySetting
import gg.essential.Essential
import gg.essential.api.gui.Slot
import gg.essential.commands.EssentialCommandRegistry
import gg.essential.config.EssentialConfig.autoUpdate
import gg.essential.config.EssentialConfig.autoUpdateState
import gg.essential.config.EssentialConfig.discordRichPresenceState
import gg.essential.config.EssentialConfig.essentialEnabledState
import gg.essential.config.EssentialConfig.friendRequestPrivacyState
import gg.essential.config.EssentialConfig.ownCosmeticsVisibleStateWithSource
import gg.essential.connectionmanager.common.packet.cosmetic.ClientCosmeticsUserEquippedVisibilityTogglePacket
import gg.essential.connectionmanager.common.packet.relationships.privacy.FriendRequestPrivacySettingPacket
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.data.OnboardingData
import gg.essential.data.OnboardingData.hasAcceptedTos
import gg.essential.elementa.components.Window
import gg.essential.gui.EssentialPalette
import gg.essential.gui.elementa.state.v2.ReferenceHolderImpl
import gg.essential.gui.elementa.state.v2.onChange
import gg.essential.gui.modal.discord.DiscordActivityStatusModal
import gg.essential.gui.modals.TOSModal
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.notification.sendTosNotification
import gg.essential.gui.vigilancev2.VigilanceV2SettingsGui
import gg.essential.util.AutoUpdate
import gg.essential.util.GuiUtil

object McEssentialConfig {
    private val referenceHolder = ReferenceHolderImpl()

    @JvmOverloads
    fun gui(initialCategory: String? = null): VigilanceV2SettingsGui = VigilanceV2SettingsGui(EssentialConfig.gui, initialCategory)

    fun hookUp() {
        EssentialConfig.doRevokeTos = ::revokeTos

        friendRequestPrivacyState.onSetValue(referenceHolder) { it ->
            if (hasAcceptedTos()) {
                val connectionManager = Essential.getInstance().connectionManager
                val privacy = FriendRequestPrivacySetting.values()[it]

                connectionManager.send(FriendRequestPrivacySettingPacket(privacy)) {
                    val get = it.orElse(null)
                    if (get == null || !(get is ResponseActionPacket && get.isSuccessful)) {
                        Notifications.error("Error", "An unexpected error occurred. Please try again.")
                    }
                }
            }
        }

        fun displayNotConnectedInformation() {
            if (hasAcceptedTos()) {
                Notifications.error(
                    "Essential Network Error",
                    "Unable to establish connection with the Essential Network."
                )
            } else {
                fun showTOS() = GuiUtil.pushModal { TOSModal(it, unprompted = false, requiresAuth = true, {}) }
                if (GuiUtil.openedScreen() == null) {
                    // Show a notification when we're not in any menu, so it's less intrusive
                    sendTosNotification { showTOS() }
                } else {
                    showTOS()
                }
            }
        }

        var lastVisibilityFromSystemSource = true

        fun restoreVisibilityFromSystemSource() {
            ownCosmeticsVisibleStateWithSource.set(lastVisibilityFromSystemSource to EssentialConfig.CosmeticsVisibilitySource.System)
        }

        ownCosmeticsVisibleStateWithSource.onChange(referenceHolder) { (visible, dataSource) ->
            val notification = when (dataSource) {
                EssentialConfig.CosmeticsVisibilitySource.UserWithNotification -> true
                EssentialConfig.CosmeticsVisibilitySource.UserWithoutNotification -> false
                // Skip system changes (e.g. infra or mod undoing change)
                EssentialConfig.CosmeticsVisibilitySource.System -> {
                    lastVisibilityFromSystemSource = visible
                    return@onChange
                }
            }
            val connectionManager = Essential.getInstance().connectionManager
            if (!connectionManager.isAuthenticated) {
                displayNotConnectedInformation()
                restoreVisibilityFromSystemSource()
                return@onChange
            }
            connectionManager.send(ClientCosmeticsUserEquippedVisibilityTogglePacket(visible)) { optionalPacket ->
                val packet = optionalPacket.orElse(null) ?: return@send run {
                    restoreVisibilityFromSystemSource()
                    Notifications.error("Error", "Failed to toggle cosmetic visibility. Please try again.")
                }

                if (packet is ResponseActionPacket && packet.isSuccessful) {
                    ownCosmeticsVisibleStateWithSource.set(visible to EssentialConfig.CosmeticsVisibilitySource.System)
                    if (notification) {
                        Notifications.push("Your cosmetics are ${if (visible) "shown" else "hidden"}", "") {
                            withCustomComponent(Slot.ICON, if (visible) EssentialPalette.COSMETICS_10X7.create() else EssentialPalette.COSMETICS_OFF_10X7.create())
                        }
                    }
                }
                // Unsuccessful packet means the correct value is already set, so do nothing
            }
        }

        discordRichPresenceState.onSetValue(referenceHolder) { enabled ->
            if (!enabled) return@onSetValue

            GuiUtil.pushModal { DiscordActivityStatusModal(it) }
        }

        essentialEnabledState.onSetValue(referenceHolder) { enabling ->
            Window.enqueueRenderOperation { toggleEssential(enabling) }
        }

        autoUpdate = AutoUpdate.autoUpdate.get()
        autoUpdateState.onSetValue(referenceHolder) { shouldAutoUpdate ->
            if (shouldAutoUpdate != AutoUpdate.autoUpdate.get()) {
                // User explicitly changed the value
                // Delayed to allow setAutoUpdate to confirm the value of the autoUpdate setting
                Window.enqueueRenderOperation {
                    AutoUpdate.setAutoUpdates(shouldAutoUpdate)
                }
            }
        }
    }

    private fun checkSPS(): Boolean {
        val currentlyHosting = Essential.getInstance().connectionManager.spsManager.localSession != null
        return if (currentlyHosting) {
            Notifications.error("Error", "You cannot disable Essential while hosting a world.")
            false
        } else true
    }

    private fun toggleEssential(enabling: Boolean) {
        // Trying to disable Essential while in an SPS world
        if (!enabling && !checkSPS()) {
            EssentialConfig.essentialEnabled = true
            return
        }

        EssentialConfig.essentialEnabled = enabling

        Essential.getInstance().keybindingRegistry.refreshBinds()
        (Essential.getInstance().commandRegistry() as EssentialCommandRegistry).checkMiniCommands()
        Essential.getInstance().checkListeners()

        if (!enabling) {
            Essential.getInstance().connectionManager.onTosRevokedOrEssentialDisabled()
        }
    }

    private fun revokeTos() {
        if (checkSPS()) {
            OnboardingData.setDeniedTos()
            Essential.getInstance().connectionManager.onTosRevokedOrEssentialDisabled()
        }
    }
}
