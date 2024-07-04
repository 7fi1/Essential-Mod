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
package gg.essential.gui.wardrobe

import gg.essential.Essential
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.FadeEffect
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.wardrobe.categories.CategoryComponent
import gg.essential.gui.wardrobe.categories.diagnosticsCategory
import gg.essential.gui.wardrobe.categories.featuredCategory
import gg.essential.gui.wardrobe.categories.outfitsCategory
import gg.essential.gui.wardrobe.categories.skinsCategory
import gg.essential.gui.wardrobe.components.banner
import gg.essential.network.connectionmanager.telemetry.TelemetryManager
import gg.essential.util.*

class WardrobeContainer(
    private val wardrobeState: WardrobeState,
) : UIContainer() {

    private val currentSuperCategory = wardrobeState.currentCategory.map { (it as? WardrobeCategory.SubCategory)?.parent ?: it }

    // FIXME: Kotlin emits invalid bytecode if this is `val`, see https://youtrack.jetbrains.com/issue/KT-48757
    var scroller: ScrollComponent
    lateinit var content: UIComponent

    init {
        val connectionManager = Essential.getInstance().connectionManager
        val noticesManager = connectionManager.noticesManager
        val draggingOnto = wardrobeState.draggingOntoEmoteSlot.map { it == -1 }
        val fadeEffect = Modifier.effect { FadeEffect(EssentialPalette.GUI_BACKGROUND, 0.5f) }
        val categoryBanner =
            noticesManager.noticeBannerManager.getNoticeBanners()
                .zip(noticesManager.saleNoticeManager.saleState).zip(currentSuperCategory) { (banners, sale), category ->
                    banners.firstOrNull { banner ->
                        (banner.categories == null || banner.categories.contains(category)) &&
                            (banner.associatedSale == null || banner.associatedSale == sale.maxByOrNull { it.discountPercent }?.name)
                    }
                }

        layout {
            column(Modifier.fillParent().whenTrue(draggingOnto, fadeEffect)) {
                // Sticky banners above the scroller
                ifNotNull(categoryBanner) { banner ->
                    if (banner.sticky) {
                        spacer(height = 10f)
                        banner(banner, Modifier.fillWidth(padding = 10f))
                    }
                }

                box(Modifier.fillWidth().fillRemainingHeight()) {
                    scroller = scrollable(Modifier.fillParent(), vertical = true) {
                        column(Modifier.fillWidth(padding = 10f).alignVertical(Alignment.Start)) {
                            // Non-sticky banners inside the scroller
                            ifNotNull(categoryBanner) { banner ->
                                if (!banner.sticky) {
                                    spacer(height = 10f)
                                    banner(banner)
                                }
                            }

                            content = box(Modifier.fillWidth()) {}
                        }
                    }

                    // Collapsible categories handle the top scroll gradient themselves (below each sub-category)
                    if_(stateOf(true) or !currentSuperCategory.map { it is WardrobeCategory.ParentCategory }) {
                        scrollGradient(scroller, true, Modifier.height(gradientHeight))
                    }
                    scrollGradient(scroller, false, Modifier.height(gradientHeight))
                }
            }

            if_(draggingOnto) {
                object : UIContainer() {
                    // Prevent children from being hovered while this component acts as a big trash can
                    override fun isPointInside(x: Float, y: Float): Boolean = true
                }(Modifier.fillParent())
            }
        }
        // TODO ideally we declare this in above `layout` but the `scroller` reference currently makes this impossible
        content.layout { bindContent() }

        wardrobeState.currentCategory.onSetValue(content) {
            if (it is WardrobeCategory.Emotes) {
                connectionManager.telemetryManager.clientActionPerformed(TelemetryManager.Actions.EMOTE_WARDROBE_SECTION_VIEWED)
            }
        }

        effect(this) {
            val itemId = wardrobeState.highlightItem()
            if (itemId != null) {
                val category = when (itemId) {
                    is ItemId.SkinItem -> WardrobeCategory.Skins
                    is ItemId.OutfitItem -> WardrobeCategory.Outfits
                    is ItemId.CosmeticOrEmote -> {
                        val currentCategory = wardrobeState.currentCategory.getUntracked()
                        if (currentCategory is WardrobeCategory.FeaturedRefresh
                            && wardrobeState.featuredPageItems.getUntracked().contains(itemId.id)
                        ) {
                            currentCategory
                        } else {
                            val cosmetic = connectionManager.cosmeticsManager.getCosmetic(itemId.id)
                            if (cosmetic == null) {
                                WardrobeCategory.Cosmetics
                            } else {
                                wardrobeState.allCategories.getUntracked().firstOrNull { it.contains(cosmetic) }
                                    ?: WardrobeCategory.Cosmetics
                            }
                        }
                    }
                    is ItemId.Bundle -> WardrobeCategory.FeaturedRefresh
                }
                this@WardrobeContainer.wardrobeState.currentCategory.set(category)
            }
        }
    }

    private fun LayoutScope.bindContent() {
        bind(currentSuperCategory, cache = true) { category ->
            when (category) {
                is WardrobeCategory.Diagnostics -> diagnosticsCategory(wardrobeState, Modifier.fillWidth())
                is WardrobeCategory.FeaturedRefresh -> featuredCategory(wardrobeState, scroller, Modifier.fillWidth())
                is WardrobeCategory.Outfits -> outfitsCategory(wardrobeState, Modifier.fillWidth())
                is WardrobeCategory.Skins -> skinsCategory(wardrobeState, Modifier.fillWidth())
                is WardrobeCategory.ParentCategory -> CategoryComponent(category, wardrobeState, scroller)()
                is WardrobeCategory.SubCategory -> throw RuntimeException("Super category should never be SubCategory")
            }
        }

        currentSuperCategory.onSetValue(content) {
            scrollingToTop = true
            scroller.scrollToTop(smoothScroll = false)
            scrollingToTop = false
        }
    }

    companion object {
        const val gradientHeight = 30f

        internal var scrollingToTop = false
    }
}
