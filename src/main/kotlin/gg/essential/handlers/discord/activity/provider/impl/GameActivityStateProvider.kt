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
package gg.essential.handlers.discord.activity.provider.impl

import gg.essential.handlers.discord.activity.ActivityState
import gg.essential.handlers.discord.activity.provider.ActivityStateProvider
import gg.essential.util.ServerType

class GameActivityStateProvider : ActivityStateProvider {
    private var state: ServerType? = null
        set(value) {

            field = value
        }

    override fun provide(): ActivityState? {
        this.state = ServerType.current()

        return when (this.state) {
            is ServerType.Singleplayer -> null

            is ServerType.Realms,
            is ServerType.Multiplayer -> ActivityState.NewMultiplayer

            is ServerType.SPS.Guest -> ActivityState.NewSPSGuest
            is ServerType.SPS.Host -> ActivityState.SPSHost

            null -> null
        }
    }
}

