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
package gg.essential.gui.layoutdsl

import gg.essential.elementa.components.inspector.Inspector
import gg.essential.gui.common.Checkbox

fun LayoutScope.checkbox(
    initialValue: Boolean,
    onValueChange: (Boolean) -> Unit
): Checkbox {
    return Checkbox(initialValue)().apply {
        isChecked.onSetValue {
            onValueChange(it)
        }
    }
}

fun LayoutScope.checkbox(
    initialValue: Boolean,
): Checkbox {
    return Checkbox(initialValue)()
}

@Suppress("unused")
private val init = run {
    Inspector.registerComponentFactory(null)
}
