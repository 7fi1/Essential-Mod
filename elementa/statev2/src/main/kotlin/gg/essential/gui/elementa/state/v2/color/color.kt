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
package gg.essential.gui.elementa.state.v2.color

import gg.essential.elementa.constraints.ColorConstraint
import gg.essential.elementa.dsl.basicColorConstraint
import gg.essential.gui.elementa.state.v2.State
import java.awt.Color

fun State<Color>.toConstraint() = basicColorConstraint { get() }

val State<Color>.constraint: ColorConstraint
    get() = toConstraint()
