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
package gg.essential.handlers

import gg.essential.Essential
import gg.essential.api.gui.Slot
import gg.essential.config.EssentialConfig
import gg.essential.config.FeatureFlags
import gg.essential.data.ABTestingData
import gg.essential.data.OnboardingData
import gg.essential.data.VersionData
import gg.essential.data.VersionInfo
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.constraints.WidthConstraint
import gg.essential.elementa.constraints.XConstraint
import gg.essential.elementa.dsl.basicXConstraint
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.coerceAtLeast
import gg.essential.elementa.dsl.coerceAtMost
import gg.essential.elementa.dsl.coerceIn
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.div
import gg.essential.elementa.dsl.minus
import gg.essential.elementa.dsl.percent
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.plus
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.state.BasicState
import gg.essential.event.essential.InitMainMenuEvent
import gg.essential.event.gui.GuiDrawScreenEvent
import gg.essential.event.gui.GuiOpenEvent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.Checkbox
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.TextFlag
import gg.essential.gui.common.and
import gg.essential.gui.common.bindConstraints
import gg.essential.gui.common.bindParent
import gg.essential.gui.common.modal.ConfirmDenyModal
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.common.not
import gg.essential.gui.common.or
import gg.essential.gui.elementa.VanillaButtonConstraint.Companion.constrainTo
import gg.essential.gui.elementa.VanillaButtonGroupConstraint.Companion.constrainTo
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.layoutdsl.Alignment
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.alignBoth
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.hoverColor
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.menu.AccountManager
import gg.essential.gui.menu.LeftSideBar
import gg.essential.gui.menu.compact.CompactRightSideBar
import gg.essential.gui.menu.full.FullRightSideBar
import gg.essential.gui.modal.sps.FirewallBlockingModal
import gg.essential.gui.modals.EssentialAutoInstalledModal
import gg.essential.gui.modals.FeaturesEnabledModal
import gg.essential.gui.modals.NotAuthenticatedModal
import gg.essential.gui.modals.TOSModal
import gg.essential.gui.modals.UpdateAvailableModal
import gg.essential.gui.modals.UpdateNotificationModal
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.error
import gg.essential.gui.notification.toastButton
import gg.essential.gui.notification.warning
import gg.essential.gui.overlay.LayerPriority
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.sps.InviteFriendsModal
import gg.essential.gui.sps.WorldSelectionModal
import gg.essential.universal.UMinecraft
import gg.essential.util.AutoUpdate
import gg.essential.util.GuiUtil
import gg.essential.util.findButtonByLabel
import gg.essential.gui.util.pollingState
import gg.essential.network.connectionmanager.serverdiscovery.NewServerDiscoveryManager
import gg.essential.network.connectionmanager.sps.SPSSessionSource
import gg.essential.universal.USound
import gg.essential.util.FirewallUtil
import gg.essential.util.MinecraftUtils
import gg.essential.util.findChildOfTypeOrNull
import gg.essential.util.isMainMenu
import gg.essential.vigilance.utils.onLeftClick
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.world.storage.WorldSummary
import java.awt.Color
import java.time.Instant
import java.util.*

//#if MC>=11600
//$$ import gg.essential.mixins.transformers.client.gui.GuiScreenAccessor
//$$ import gg.essential.util.textTranslatable
//$$ import net.minecraft.client.gui.screen.MultiplayerWarningScreen
//$$ import net.minecraft.client.gui.widget.Widget
//#endif

class PauseMenuDisplay {

    private val fullRightMenuPixelWidth = 104.pixels
    private val collapsedRightMenuPixelWidth = 68.pixels
    private val rightMenuMinPadding = 5
    private var init = false

    fun init(screen: GuiScreen) {
        init = true

        if (screen.isMainMenu) {
            Essential.EVENT_BUS.post(InitMainMenuEvent())
        }

        if (EssentialConfig.essentialFull) {

            val menuType =
                if (screen.isMainMenu) MenuType.MAIN
                else if (UMinecraft.getMinecraft().currentServerData != null) MenuType.SERVER
                else MenuType.SINGLEPLAYER

            // Create containers around the top and bottom buttons, so we can use them for GUI alignment
            val topButton by UIContainer().constrainTo(
                listOf(
                    screen.findButtonByLabel("menu.singleplayer", "menu.returnToGame"),
                    screen.findButtonByLabel("menu.multiplayer")
                )
            ) {
                x = CenterConstraint()
                y = 25.percent + 48.pixels
                width = 200.pixels
                height = 20.pixels
            } childOf window
            val bottomButton by UIContainer().constrainTo(
                screen.findButtonByLabel("menu.quit", "menu.returnToMenu", "menu.disconnect", "replaymod.gui.exit")
            ) {
                x = CenterConstraint()
                y = SiblingConstraint(64f)
                width = 200.pixels
                height = 20.pixels
            } childOf window

            val isCompact = BasicState(EssentialConfig.essentialMenuLayout == 1)

            val collapsed = bottomButton.pollingState {
                    getRightSideMenuX(topButton, fullRightMenuPixelWidth).getXPosition(window) +
                        fullRightMenuPixelWidth.value + rightMenuMinPadding >= window.getRight()
                }

            val menuVisible = bottomButton.pollingState { EssentialConfig.essentialMenuLayout != 2 }

            val rightContainer by UIContainer().constrain {
                height = ChildBasedMaxSizeConstraint()
            }.bindConstraints(collapsed.zip(isCompact)) { (collapse, isCompact) ->
                if (isCompact) {
                    x = if (EssentialConfig.closerMenuSidebar) {
                        (13.pixels(alignOpposite = true) boundTo window).coerceIn(
                            (0.pixels(alignOpposite = true) boundTo topButton) + 24.pixels,
                            (SiblingConstraint(maxSpaceBetweenSides + 20f) boundTo topButton) - basicXConstraint { it.getWidth() },
                        )
                    } else {
                        (13.pixels(alignOpposite = true) boundTo window)
                            .coerceAtLeast((0.pixels(alignOpposite = true) boundTo topButton) + 24.pixels)
                    }.coerceAtMost(rightMenuMinPadding.pixels(alignOpposite = true) boundTo window)
                    y = (((CenterConstraint() boundTo bottomButton) + (CenterConstraint() boundTo topButton)) / 2)
                            .coerceAtMost(40.pixels(alignOpposite = true) boundTo window)
                            .coerceAtLeast(0.pixels(alignOpposite = true) boundTo bottomButton)
                    width = ChildBasedSizeConstraint()
                } else {
                    width = if (collapse) collapsedRightMenuPixelWidth else fullRightMenuPixelWidth
                    x = getRightSideMenuX(topButton, width).coerceAtMost(rightMenuMinPadding.pixels(alignOpposite = true) boundTo window)
                    y = 28.pixels.coerceAtLeast((0.pixels boundTo topButton) - 100.pixels)
                }
            } childOf window

            bottomButton.addUpdateFunc { _, _ ->
                isCompact.set(
                    getRightSideMenuX(topButton, collapsedRightMenuPixelWidth).getXPosition(rightContainer) +
                        collapsedRightMenuPixelWidth.value + rightMenuMinPadding >= window.getRight()
                        || EssentialConfig.essentialMenuLayout == 1
                )
            }

            val leftContainer by UIContainer().constrain {
                width = 50.percent
                height = 100.percent
            } childOf window

            val accountManager = AccountManager()
            CompactRightSideBar(menuType, menuVisible, rightContainer, accountManager)
                .bindParent(rightContainer, menuVisible and isCompact)
            FullRightSideBar(menuType, topButton, bottomButton, collapsed, menuVisible and !isCompact)
                .bindParent(rightContainer, menuVisible and !isCompact)

            LeftSideBar(topButton, bottomButton, menuVisible.toV2(), collapsed.toV2(), isCompact.toV2(), menuType, rightContainer, leftContainer, accountManager)
                .bindParent(leftContainer, menuVisible)

            if (menuType == MenuType.MAIN
                && Instant.now() < NewServerDiscoveryManager.NEW_TAG_END_DATE
                && !OnboardingData.seenServerDiscovery.getUntracked()
            ) {
                val multiplayerButton = UIContainer().constrainTo(
                    screen.findButtonByLabel("menu.multiplayer")
                ) {
                    x = CenterConstraint()
                    y = 25.percent + 52.pixels
                    width = 200.pixels
                    height = 20.pixels
                } childOf window

                TextFlag(
                    stateOf(MenuButton.NOTICE_GREEN),
                    MenuButton.Alignment.CENTER,
                    stateOf("NEW")
                ).constrain {
                    y = CenterConstraint() boundTo multiplayerButton
                    x = 3.pixels(alignOpposite = true, alignOutside = true) boundTo multiplayerButton
                }.onLeftClick {
                    if (OnboardingData.hasAcceptedTos()) {
                        EssentialConfig.currentMultiplayerTab = 2
                    }
                    //#if MC>=11600
                    //$$ if (UMinecraft.getMinecraft().gameSettings.skipMultiplayerWarning) {
                    //$$     GuiUtil.openScreen { MultiplayerScreen(screen) }
                    //$$ } else {
                    //$$     GuiUtil.openScreen { MultiplayerWarningScreen(screen) }
                    //$$ }
                    //#else
                    GuiUtil.openScreen { GuiMultiplayer(screen) }
                    //#endif
                } childOf window
            }
        }

        EssentialAutoInstalledModal.showModal()

        // Update available toast
        if (AutoUpdate.updateAvailable.get() && !AutoUpdate.seenUpdateToast && !AutoUpdate.updateIgnored.get()) {
            fun showUpdateToast(message: String? = null) {
                var updateClicked = false

                val updateButton = toastButton("Update",
                    backgroundModifier = Modifier.color(EssentialPalette.GREEN_BUTTON)
                        .hoverColor(EssentialPalette.GREEN_BUTTON_HOVER)
                        .shadow(Color.BLACK),
                    textModifier = Modifier.color(EssentialPalette.TEXT_HIGHLIGHT)
                        .shadow(EssentialPalette.TEXT_SHADOW)
                )

                Notifications.pushPersistentToast(AutoUpdate.getNotificationTitle(false), message ?: " ", {
                    GuiUtil.pushModal { manager -> UpdateAvailableModal(manager) }
                }, {
                    if (!updateClicked) {
                        AutoUpdate.ignoreUpdate()
                    }
                }, {
                    withCustomComponent(Slot.ACTION, updateButton)
                    trimMessage = true
                    AutoUpdate.dismissUpdateToast = {
                        updateClicked = true
                        dismissNotification()
                    }
                })
            }

            AutoUpdate.changelog.whenCompleteAsync({ changelog, _ -> showUpdateToast(changelog) }, Window::enqueueRenderOperation)

            AutoUpdate.seenUpdateToast = true
        }

        // Update Notification Modal
        if (VersionData.getMajorComponents(VersionData.essentialVersion) != VersionData.getMajorComponents(VersionData.getLastSeenModal())
            && EssentialConfig.updateModal
        ) {
            if (VersionData.getLastSeenModal() == VersionInfo.noSavedVersion) {
                // If first launch, update last seen modal and don't show changelog
                VersionData.updateLastSeenModal()
            } else {
                GuiUtil.queueModal(UpdateNotificationModal(GuiUtil))
            }
        }

        // AB Features Enabled Modal
        if (FeatureFlags.abTestingFlags
                .filterValues { featureData -> featureData.second }
                .filterKeys { name -> !ABTestingData.hasData("Notified:$name") }
                .isNotEmpty()
        ) {
            GuiUtil.queueModal(FeaturesEnabledModal(GuiUtil))
        }
    }

    @Subscribe
    fun guiOpen(event: GuiOpenEvent) {
        refresh()
    }

    @Subscribe
    fun drawScreen(event: GuiDrawScreenEvent) {
        val screen = event.screen
        if (screen !is GuiIngameMenu && !screen.isMainMenu) {
            return
        }

        //#if MC>=11600
        //$$ if (screen is IngameMenuScreen && screen is GuiScreenAccessor &&
        //$$     (screen.`essential$getChildren`().isEmpty() || (screen.`essential$getChildren`().size == 1 &&
        //$$             screen.`essential$getChildren`().any { it is Widget && it.message == textTranslatable("menu.paused") } ))) {
        //$$     return // F3+Esc
        //$$ }
        //#endif

        if (!init) {
            init(screen)
        }
    }

    fun refresh() {
        window.clearChildren()
        init = false
    }

    private fun getRightSideMenuX(topButton: UIContainer, width: WidthConstraint): XConstraint {
        // This maintains the appropriate distance from both the accessibility button and edges of the screen as much as possible
        return ((SiblingConstraint() boundTo topButton) +
            (((0.pixels(alignOpposite = true) boundTo window) - (0.pixels(alignOpposite = true) boundTo topButton)) / 2f - (width / 2)))
            .coerceIn(
                SiblingConstraint(28f) boundTo topButton,
                SiblingConstraint(65f) boundTo topButton
            )
    }

    companion object {
        var window: Window = GuiUtil.createPersistentLayer(LayerPriority.AboveScreenContent).window

        @JvmStatic
        val minWidth = 404
        @JvmStatic
        val maxSpaceBetweenSides = 187f

        @JvmStatic
        fun canRescale(screen: GuiScreen): Boolean {
            return (screen.isMainMenu || screen is GuiIngameMenu)
        }

        // Opens the appropriate SPS/invite modal based on the user's current connection
        @JvmStatic
        @JvmOverloads
        fun showInviteOrHostModal(
            source: SPSSessionSource,
            prepopulatedInvites: Set<UUID> = emptySet(),
            worldSummary: WorldSummary? = null,
            previousModal: Modal? = null,
            showIPWarning: Boolean = true,
            callback: () -> Unit = {},
        ) {
            val connectionManager = Essential.getInstance().connectionManager
            val currentServerData = UMinecraft.getMinecraft().currentServerData
            val spsManager = connectionManager.spsManager

            // Attempts to replace the previously opened modal, or, push a new modal if one is not open.
            fun pushModal(builder: (ModalManager) -> Modal) {
                if (previousModal != null) {
                    previousModal.replaceWith(builder(previousModal.modalManager))
                } else {
                    GuiUtil.pushModal(builder)
                }
            }

            // Attempts to show the user various warnings (TOS, Connection Manager, Firewall, etc.) before pushing
            // the provided modal.
            fun pushModalAndWarnings(
                showNetworkRelatedWarnings: Boolean,
                builder: (ModalManager) -> Modal
            ) {
                fun Modal.retryModal(showIPWarningOverride: Boolean = showIPWarning) {
                    showInviteOrHostModal(
                        source,
                        prepopulatedInvites,
                        worldSummary,
                        this,
                        showIPWarningOverride,
                        callback,
                    )
                }

                if (!OnboardingData.hasAcceptedTos()) {
                    pushModal { manager ->
                        TOSModal(
                            manager,
                            requiresAuth = true,
                            confirmAction = { retryModal() }
                        )
                    }

                    return
                }

                if (!connectionManager.isAuthenticated) {
                    pushModal { manager ->
                        NotAuthenticatedModal(manager, successCallback = { retryModal() })
                    }

                    return
                }

                if (showNetworkRelatedWarnings) {
                    if (FirewallUtil.isFirewallBlocking()) {
                        pushModal { manager ->
                            FirewallBlockingModal(manager, null, tryAgainAction = { retryModal() })
                        }

                        return
                    }

                    if (showIPWarning && EssentialConfig.spsIPWarning) {
                        pushModal { manager ->
                            createIPAddressWarningModal(manager, callback = { retryModal(false) })
                        }

                        return
                    }
                }

                // All warnings/checks have been performed, we can show the original modal.
                pushModal(builder)
            }

            if (Minecraft.getMinecraft().currentScreen.isMainMenu && worldSummary == null) {
                // The world selection modal does not get any network warnings, those will be shown
                // in the later stage of the modal (see where `worldSummary != null`).
                pushModalAndWarnings(showNetworkRelatedWarnings = false) { WorldSelectionModal(it) }
                return
            }

            if (UMinecraft.getMinecraft().integratedServer != null) {
                if (MinecraftUtils.isHostingSPS()) {
                    pushModalAndWarnings(showNetworkRelatedWarnings = false) { manager ->
                        InviteFriendsModal.createSelectFriendsModal(
                            manager,
                            spsManager.invitedUsers + prepopulatedInvites,
                            justStarted = false,
                            onComplete = callback,
                        )
                    }
                } else {
                    pushModalAndWarnings(showNetworkRelatedWarnings = true) { manager ->
                        spsManager.startLocalSession(source)

                        InviteFriendsModal.createWorldSettingsModal(
                            manager,
                            prepopulatedInvites,
                            justStarted = true,
                            source = source,
                            callbackAfterOpen = callback,
                        )
                    }
                }
            } else if (currentServerData != null) {
                val serverAddress = currentServerData.serverIP
                val isSPSServer = connectionManager.spsManager.isSpsAddress(serverAddress)
                if (isSPSServer) {
                    Notifications.warning("Only hosts can send invites", "")
                    return
                }

                pushModalAndWarnings(showNetworkRelatedWarnings = false) { manager ->
                    InviteFriendsModal.showInviteModal(
                        manager,
                        source = source,
                        onComplete = callback
                    )
                }
            } else if (worldSummary != null) {
                pushModalAndWarnings(showNetworkRelatedWarnings = true) { manager ->
                    InviteFriendsModal.createWorldSettingsModal(
                        manager,
                        prepopulatedInvites,
                        justStarted = true,
                        worldSummary,
                        source = source,
                    )
                }
            } else {
                // Realms, ReplayMod, etc.
                Notifications.error("Can't invite to this world", "")
            }
        }

        fun createIPAddressWarningModal(modalManager: ModalManager, callback: Modal.() -> Unit): Modal {
            return ConfirmDenyModal(
                modalManager,
                false
            ).configure {
                titleText =
                    "This world will be hosted through your internet. " +
                        "Your host's IP will be visible through network logs! \n\nDo you want to proceed?"
                primaryButtonText = "Proceed"
                spacer.setHeight(12.pixels)

                onPrimaryAction { callback(this) }
            }.configureLayout { customContent ->
                customContent.layout {
                    val checkBoxState = !EssentialConfig.spsIPWarningState
                    column(Modifier.alignBoth(Alignment.Center)) {
                        row(Modifier.hoverScope(), Arrangement.spacedBy(5f)) {
                            val notifyToggle by Checkbox(checkmarkColor = BasicState(EssentialPalette.TEXT)).constrain {
                                width = 9.pixels
                                height = AspectConstraint()
                            }
                            notifyToggle.isChecked.onSetValue {
                                checkBoxState.set(it)
                            }
                            notifyToggle()
                            text("Do not show this warning again", shadow = false, modifier = Modifier.color(EssentialPalette.TEXT_DISABLED))
                        }.onLeftClick {
                            it.stopPropagation()
                            USound.playButtonPress()
                            findChildOfTypeOrNull<Checkbox>(true)?.toggle()
                        }
                        spacer(height = 14f)
                    }
                }
            }
        }
    }

    enum class MenuType { MAIN, SINGLEPLAYER, SERVER }
}
