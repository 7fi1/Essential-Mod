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
import gg.essential.gui.layoutdsl.LayoutScope
import gg.essential.gui.layoutdsl.Modifier
import gg.essential.gui.layoutdsl.text
import gg.essential.util.CachedAvatarImage
import gg.essential.util.UuidNameLookup
import java.util.*

fun LayoutScope.playerAvatar(uuid: UUID, modifier: Modifier = Modifier) {
    val image = CachedAvatarImage.ofUUID(uuid)
        .effect(ShadowEffect())

    image(modifier)
}

fun LayoutScope.playerName(uuid: UUID, modifier: Modifier = Modifier) {
    text(UuidNameLookup.nameState(uuid, "Loading..."), modifier)
}
