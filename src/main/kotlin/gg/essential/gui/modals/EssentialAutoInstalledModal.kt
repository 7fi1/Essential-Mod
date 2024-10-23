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
package gg.essential.gui.modals

import gg.essential.config.EssentialConfig
import gg.essential.elementa.constraints.CopyConstraintFloat
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.boundTo
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.dsl.provideDelegate
import gg.essential.elementa.dsl.width
import gg.essential.elementa.state.BasicState
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.MenuButton
import gg.essential.gui.common.modal.EssentialModal
import gg.essential.gui.common.modal.configure
import gg.essential.gui.layoutdsl.Arrangement
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.color
import gg.essential.gui.layoutdsl.column
import gg.essential.gui.layoutdsl.fillWidth
import gg.essential.gui.layoutdsl.hoverScope
import gg.essential.gui.layoutdsl.hoverTooltip
import gg.essential.gui.layoutdsl.layout
import gg.essential.gui.layoutdsl.row
import gg.essential.gui.layoutdsl.shadow
import gg.essential.gui.layoutdsl.spacer
import gg.essential.gui.layoutdsl.text
import gg.essential.gui.layoutdsl.underline
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.util.layoutSafePollingState
import gg.essential.universal.ChatColor
import gg.essential.util.EssentialContainerUtil
import gg.essential.util.GuiUtil
import gg.essential.util.ModLoaderUtil
import gg.essential.util.openInBrowser
import java.awt.Color
import java.net.URI

class EssentialAutoInstalledModal(modalManager: ModalManager) : EssentialModal(modalManager) {

    init {
        configure {
            primaryButtonText = "Learn More"
            titleText = "Essential Mod has been installed\nbecause its libraries are required"
            titleTextColor = EssentialPalette.TEXT
            primaryButtonAction = {
                openInBrowser(URI("https://essential.gg/wiki/installed-by-other-mods"))
            }
        }

        onPrimaryOrDismissAction {
            updatePreviouslyLaunchedWithContainer()
        }

        val okayButton by MenuButton(
            "Okay", defaultStyle = BasicState(MenuButton.BLUE), hoverStyle = BasicState(MenuButton.LIGHT_BLUE)
        ) {
            replaceWith(null)
            updatePreviouslyLaunchedWithContainer()
        }.constrain {
            x = SiblingConstraint(8f)
            width = CopyConstraintFloat() boundTo primaryActionButton
            height = 20.pixels
        }

        buttonContainer.insertChildAfter(okayButton, primaryActionButton)

        val modsDependingOnEssential = ModLoaderUtil.getModsDependingOnEssential().map { it.name }

        val modText = when (modsDependingOnEssential.size) {
            0 -> "by a mod you installed."
            1 -> "by ${ChatColor.WHITE}${modsDependingOnEssential.first()}${ChatColor.RESET}."
            else -> "to run ${ChatColor.WHITE}${modsDependingOnEssential.joinToString()}${ChatColor.RESET}."
        }
        val widthState = customContent.layoutSafePollingState(customContent.getWidth()) { customContent.getWidth() }
        val modTextModifier = Modifier.color(EssentialPalette.TEXT).shadow(EssentialPalette.BLACK)

        configureLayout {
            it.layout {
                column(Modifier.fillWidth()) {
                    bind(widthState) { width ->
                        if (modText.width() < width) {
                            text(modText, modTextModifier)
                        } else {
                            row(Modifier.fillWidth(), Arrangement.spacedBy(1f)) {
                                text("to run ", modTextModifier)
                                text("these mods", modTextModifier.underline().color(Color.WHITE)
                                    .hoverTooltip(modsDependingOnEssential.joinToString(), wrapAtWidth = width, padding = 7f)
                                    .hoverScope())
                                text(".", modTextModifier)
                            }
                        }
                    }
                    spacer(height = 20f)
                }
            }
        }
    }

    companion object {
        fun showModal() {
            if (!shouldShowModal()) {
                updatePreviouslyLaunchedWithContainer()
                return
            }
            GuiUtil.queueModal(EssentialAutoInstalledModal(GuiUtil))
        }

        private fun shouldShowModal(): Boolean {
            val previouslyLaunchedWithContainer = EssentialConfig.previouslyLaunchedWithContainer.get()
            return (previouslyLaunchedWithContainer == EssentialConfig.PreviouslyLaunchedWithContainer.Yes
                || previouslyLaunchedWithContainer == EssentialConfig.PreviouslyLaunchedWithContainer.Unknown)
                && !EssentialContainerUtil.isContainerPresent()
        }

        private fun updatePreviouslyLaunchedWithContainer() {
            EssentialConfig.previouslyLaunchedWithContainer.set(
                if (EssentialContainerUtil.isContainerPresent()) {
                    EssentialConfig.PreviouslyLaunchedWithContainer.Yes
                } else {
                    EssentialConfig.PreviouslyLaunchedWithContainer.No
                }
            )
        }
    }

}