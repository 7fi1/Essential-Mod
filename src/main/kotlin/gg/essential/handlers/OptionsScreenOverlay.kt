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

import gg.essential.config.EssentialConfig
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.constraints.AspectConstraint
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.event.gui.GuiDrawScreenEvent
import gg.essential.event.gui.GuiOpenEvent
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.MenuButton
import gg.essential.gui.elementa.VanillaButtonConstraint.Companion.constrainTo
import gg.essential.gui.overlay.Layer
import gg.essential.gui.overlay.LayerPriority
import gg.essential.util.GuiUtil
import gg.essential.util.ModLoaderUtil
import gg.essential.util.findButtonByLabel
import gg.essential.gui.util.hoveredState
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiScreen

class OptionsScreenOverlay {

    private var layer: Layer? = null

    fun init(screen: GuiScreen) {
        val layer = GuiUtil.createPersistentLayer(LayerPriority.AboveScreenContent).also {
            this.layer = it
        }

        val window = layer.window

        // This mod removes the telemetry button, causing the credits and attribution button to move to the left.
        // This makes our options button look weird, so let's put it back to the position that it was in on 1.19.2.
        // Linear issue for reference: EM-1802
        val positionOn11904AndHigher = if (ModLoaderUtil.isModLoaded("nochatreports")) {
            "options.accessibility.title"
        } else {
            "options.credits_and_attribution"
        }

        val bottomRightButton by UIContainer().constrainTo(
            screen.findButtonByLabel(
                "options.snooper.view", // 1.8.9 - 1.12.2
                "options.accessibility.title", // 1.16.2 - 1.19.2
                positionOn11904AndHigher, // 1.19.4+
            )
        ) {
            // if we can't find the button, just assume it's centered vertically
            x = 50.percent + 5.pixels
            y = CenterConstraint()
            width = 150.pixels
            height = 20.pixels
        } childOf window

        val settingsButton by MenuButton(shouldBeRetextured = true) {
            GuiUtil.openScreen { EssentialConfig.gui() }
        }.constrain {
            x = SiblingConstraint(4f) boundTo bottomRightButton
            y = 0.pixels boundTo bottomRightButton
            width = 20.pixels
            height = AspectConstraint()
        }.setIcon(BasicState(EssentialPalette.SETTINGS_9X8)) childOf window

        val settingsTooltip by EssentialTooltip(
            settingsButton,
            position = EssentialTooltip.Position.ABOVE,
        ).constrain {
            x = (CenterConstraint() boundTo settingsButton) coerceAtMost 6.pixels(alignOpposite = true)
            y = (SiblingConstraint(4f, alignOpposite = true) boundTo settingsButton) - 1.pixels
        }.addLine("Essential Settings")
            .bindVisibility(settingsButton.hoveredState())
    }

    @Subscribe
    fun guiOpen(event: GuiOpenEvent) {
        layer?.also {
            GuiUtil.removeLayer(it)
            layer = null
        }
    }

    @Subscribe
    fun drawScreen(event: GuiDrawScreenEvent) {
        if (event.screen !is GuiOptions) {
            return
        }

        if (layer == null) {
            init(event.screen)
        }
    }
}