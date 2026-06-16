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
package gg.essential.handlers.discord.activity

/**
 * The different 'activity states' that can be sent when using the Discord Integration
 */
sealed class ActivityState {
    /**
     * The text which is shown under "Playing Minecraft" on the user's Discord Presence
     */
    abstract val text: String

    object NewMultiplayer : ActivityState() {
        override val text = "On Server"
    }

    /**
     * Shown when the user is hosting an SPS session on single player
     *
     * @param partyInfo The information about the party
     */
    object SPSHost : ActivityState() {
        override val text = "Hosting World"
    }

    object NewSPSGuest : ActivityState() {
        override val text = "In World"
    }

}
