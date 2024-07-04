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
package gg.essential.gui.modals.select.component

import gg.essential.elementa.dsl.effect
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.friends.previews.ChannelPreview
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier

fun LayoutScope.groupIcon(id: Long, modifier: Modifier = Modifier) {
    val image = ChannelPreview.newGroupIcon(id).effect(ShadowEffect())

    image(modifier)
}
