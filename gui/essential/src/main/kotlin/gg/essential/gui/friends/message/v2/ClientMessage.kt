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
package gg.essential.gui.friends.message.v2

import com.sparkuniverse.toolbox.chat.model.Channel
import com.sparkuniverse.toolbox.chat.model.InviteMessageContent
import com.sparkuniverse.toolbox.chat.model.MessageContent
import com.sparkuniverse.toolbox.chat.model.MessageContent.CosmeticGift
import com.sparkuniverse.toolbox.chat.model.MessageContent.Media
import com.sparkuniverse.toolbox.chat.model.MessageContent.Plain
import com.sparkuniverse.toolbox.chat.model.MessageContent.ServerInvite
import com.sparkuniverse.toolbox.chat.model.MessageContent.SPSInvite
import com.sparkuniverse.toolbox.chat.model.MessageContent.Unknown
import gg.essential.cosmetics.CosmeticId
import gg.essential.gui.friends.message.MessageUtils
import gg.essential.gui.friends.message.MessageUtils.handleMarkdownUrls
import gg.essential.mod.Skin
import gg.essential.network.cosmetics.toMod
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

data class ClientMessage(
    val id: Long,
    val channel: Channel,
    val sender: UUID,
    val content: MessageContent,
    val sendState: SendState,
    val replyTo: MessageRef?,
    val lastEditTime: Long?,
    val createdAt: Long,
) {
    val sendTime: Instant = Instant.ofEpochMilli(createdAt)
    val sent = sendState == SendState.Confirmed
    val parts: List<Part> = buildList {
        when (content) {
            is Plain -> {
                add(0, Part.Text(
                    content.unfilteredText.handleMarkdownUrls(),
                    content.text.handleMarkdownUrls(),
                ))
            }

            is Media -> {
                for (mediaId in content.mediaIds) {
                    add(Part.Image(mediaId))
                }
            }

            is SPSInvite -> {
                add(Part.InvitePart(content))
            }

            is ServerInvite -> {
                add(Part.InvitePart(content))
            }

            is CosmeticGift -> {
                add(Part.Gift(content.cosmeticId))
            }

            is MessageContent.Skin -> {
                add(Part.Skin(Skin(content.hash, content.model.toMod())))
            }

            is Unknown -> {
                add(Part.Text("Message Unavailable: Please update Essential"))
            }
        }
    }

    sealed interface Part {
        data class Text(val unfilteredContent: String, val filteredContent: String) : Part {
            constructor(content: String) : this(content, content)
        }
        data class Image(val id: String) : Part
        data class Gift(val id: CosmeticId) : Part
        data class InvitePart(val invite: InviteMessageContent) : Part
        data class Skin(val skin: gg.essential.mod.Skin) : Part
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClientMessage::class.java)
    }
}

sealed interface SendState {

    data object Sending : SendState

    data object Confirmed : SendState

    data object Failed : SendState

    data class Blocked(val reason: String) : SendState {

        val toastMessage = when(reason) {
            CONTAINS_NON_WHITELISTED_DOMAIN -> "You cannot share this link"
            else -> "You cannot send this message"
        }

        val tooltipMessage = when(reason) {
            CONTAINS_NON_WHITELISTED_DOMAIN -> "Message not sent: This link is not allowed"
            else -> "Message not sent: This isn't allowed"
        }

        companion object {

            private const val CONTAINS_NON_WHITELISTED_DOMAIN = "CONTAINS_NON_WHITELISTED_DOMAIN"
        }
    }
}