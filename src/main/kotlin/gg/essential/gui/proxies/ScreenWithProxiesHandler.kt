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
package gg.essential.gui.proxies

import gg.essential.elementa.UIComponent
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.box
import gg.essential.mixins.transformers.client.gui.GuiScreenAccessor
import gg.essential.universal.UMinecraft
import net.minecraft.client.gui.GuiScreen

class ScreenWithProxiesHandler(
    private val screen: GuiScreen,
    private val buttonIds: Map<String, Int>,
    private val flagIds: Map<String, Int>,
    private val playerIds: Map<String, Int>,
) {
    private val proxyIds = buttonIds.keys + flagIds.keys + playerIds.keys
    private val access = screen as GuiScreenAccessor

    fun initGui() {
        // for reasoning see [EssentialProxyElement.init]
        // for values see fancy menu github:
        // v2 https://github.com/Keksuccino/FancyMenu/blob/4b85ac0906d8b1862312779d1efe5ec48b8dec31/src/main/java/de/keksuccino/fancymenu/menu/button/ButtonCache.java#L126
        // v3 https://github.com/Keksuccino/FancyMenu/blob/57564b4891ab83fc258377d03db8a857a39c91b8/common/src/main/java/de/keksuccino/fancymenu/customization/widget/ScreenWidgetDiscoverer.java#L42
        val isProbablyFancyMenuIdentifierPass = screen.width == 1000 && screen.height == 1000

        buttonIds.forEach { addProxy(MenuButtonProxy(it.key, isProbablyFancyMenuIdentifierPass, it.value)) }
        flagIds.forEach { addProxy(NoticeFlagProxy(it.key, isProbablyFancyMenuIdentifierPass, it.value)) }
        playerIds.forEach { addProxy(UIPlayerProxy(it.key, isProbablyFancyMenuIdentifierPass, it.value)) }
    }

    private fun addProxy(proxy: EssentialProxyElement<*>) {
        //#if MC>=11700
        //$$ access.`essential$addDrawableChild`(proxy)
        //#else
        access.buttonList.add(proxy)
        //#endif
    }

    fun getProxy(id: String): EssentialProxyElement<*>? {
        if (id !in proxyIds) return null

        //#if MC>=11700
        //$$ return access.drawables
        //#else
        return access.buttonList
        //#endif
            .filterIsInstance<EssentialProxyElement<*>>()
            .firstOrNull { it.essentialId == id }
    }

    companion object {

        /** Main / Pause Menus */
        @JvmStatic
        fun forMainMenu(screen: GuiScreen) : ScreenWithProxiesHandler =
            ScreenWithProxiesHandler(screen, mainMenuButtons, mainMenuFlags, mainAndPauseMenuPlayers)

        @JvmStatic
        fun forPauseMenu(screen: GuiScreen) : ScreenWithProxiesHandler =
            ScreenWithProxiesHandler(screen, pauseMenuButtons, pauseMenuFlags, mainAndPauseMenuPlayers)

        fun LayoutScope.mountWithProxy(id: String, modifier: Modifier = Modifier, block: LayoutScope.() -> Unit) {
            val mounted = mutableStateOf(stateOf(true))
            lateinit var container: UIComponent
            if_({ mounted()() }) {
                // use box to create a container to hold the components and send to the proxies
                container = box(modifier) {
                    block() // may setup any components, even none or conditional ones
                }
            }
            getProxyFromCurrentScreen(id)?.acceptNewEssentialContainer(container, mounted)
        }

        @Suppress("MemberVisibilityCanBePrivate") // used in 1.18+
        fun currentProxyScreenOrNull(): ScreenWithVanillaProxyElementsExt? = UMinecraft.currentScreenObj as? ScreenWithVanillaProxyElementsExt

        private fun getProxyFromCurrentScreen(id: String): EssentialProxyElement<*>? {
            return currentProxyScreenOrNull()?.`essential$getProxyHandler`()?.getProxy(id)
        }

        // main menu components
        // the numbers are hardcoded as they are essential (heh) to allowing fancy menu to consistently identify the buttons
        // so we MUST ensure that the numbers remain the same even if elements are changed in future updates
        // if these were inconsistent (e.g. driven by index then entries changed) then exising fancy menu layouts would misidentify the buttons and break
        // while these are arbitrary they will also represent where our disabled/inactive buttons will appear in the fancy menu editor
        // at position [val * 2, val], and should remain small
        private val mainMenuButtons = mapOf(
            "reserved_0" to 0,
            "invite_host" to 2,
            "world_host" to 3,
            "social" to 4,
            "wardrobe" to 5,
            "wardrobe_2" to 6,
            "pictures" to 7,
            "settings" to 8,
            "account" to 9,
            )
        private val mainMenuFlags = mapOf(
            "reserved_10" to 10,
            "beta" to 11,
            "update" to 12,
            "message" to 13,
        )
        private val mainAndPauseMenuPlayers = mapOf(
            "player" to 14, // only 1 entry for now, but still use a map for consistency
        )

        private val pauseMenuButtons = mapOf(
            "invite_host" to 2,
            "world_host" to 3,
            "social" to 4,
            "wardrobe" to 5,
            "wardrobe_2" to 6,
            "pictures" to 7,
            "settings" to 8,
        )
        private val pauseMenuFlags = mapOf(
            "beta" to 11,
            "update" to 12,
            "message" to 13,
        )
    }
}