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
package gg.essential.gui.common

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer

class ScrollSpacer(val top: Boolean) : UIContainer() {

    companion object {
        val comparator: Comparator<UIComponent> = compareBy {
            when (it) {
                is ScrollSpacer -> if (it.top) -1 else 1
                else -> 0
            }
        }
    }
}