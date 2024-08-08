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

import gg.essential.elementa.state.State
import gg.essential.gui.common.onSetValueAndNow
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.effect
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.elementa.state.v2.State as StateV2

@Deprecated("Using StateV1 is discouraged, use StateV2 instead")
fun Modifier.then(state: State<Modifier>): Modifier {
    return this then {
        var reverse: (() -> Unit)? = null

        val cleanupState = state.onSetValueAndNow {
            reverse?.invoke()
            reverse = it.applyToComponent(this)
        };

        {
            cleanupState()
            reverse?.invoke()
            reverse = null
        }
    }
}

fun Modifier.then(state: StateV2<Modifier>): Modifier {
    return this then component@{
        var reverse: (() -> Unit)? = null

        val cleanupState = effect(this) {
            reverse?.invoke()
            reverse = state().applyToComponent(this@component)
        };

        {
            cleanupState()
            reverse?.invoke()
            reverse = null
        }
    }
}

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Using StateV1 is discouraged, use StateV2 instead")
fun Modifier.whenTrue(state: State<Boolean>, activeModifier: Modifier, inactiveModifier: Modifier = Modifier): Modifier =
    then(state.toV2().map { if (it) activeModifier else inactiveModifier })

fun Modifier.whenTrue(state: StateV2<Boolean>, activeModifier: Modifier, inactiveModifier: Modifier = Modifier): Modifier =
    then(state.map { if (it) activeModifier else inactiveModifier })