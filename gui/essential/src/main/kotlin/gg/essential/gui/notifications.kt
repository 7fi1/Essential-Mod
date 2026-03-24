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
package gg.essential.gui

import gg.essential.api.gui.Slot
import gg.essential.elementa.dsl.*
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.notification.Notifications

// Easy access from java
fun sendPictureCopiedNotification() {
    Notifications.push("Picture copied to clipboard", "") {
        withCustomComponent(Slot.ICON, EssentialPalette.PICTURES_SHORT_9X7.create())
    }
}

@JvmOverloads
fun sendCheckmarkNotification(
    message: String,
    action: () -> Unit = {},
) {
    Notifications.push(message, "", action = action) {
        withCustomComponent(Slot.ICON, EssentialPalette.CHECKMARK_7X5.create())
    }
}
