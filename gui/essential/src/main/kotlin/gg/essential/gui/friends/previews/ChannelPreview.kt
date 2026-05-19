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
package gg.essential.gui.friends.previews

import com.sparkuniverse.toolbox.chat.model.Channel
import com.sparkuniverse.toolbox.chat.model.MessageContent.Media
import gg.essential.config.EssentialConfig
import gg.essential.cosmetics.CosmeticId
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.ContextOptionMenu
import gg.essential.gui.common.shadow.ShadowEffect
import gg.essential.gui.elementa.state.v2.Observer
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.combinators.and
import gg.essential.gui.elementa.state.v2.combinators.map
import gg.essential.gui.elementa.state.v2.combinators.not
import gg.essential.gui.elementa.state.v2.combinators.or
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.mutableStateOf
import gg.essential.gui.elementa.state.v2.stateOf
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.elementa.state.v2.toV2
import gg.essential.gui.friends.message.v2.ClientMessage
import gg.essential.gui.friends.state.PlayerActivity
import gg.essential.gui.friends.state.SocialStates
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.studio.Tag
import gg.essential.gui.util.hoveredState
import gg.essential.universal.USound
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.vigilance.utils.onLeftClick
import org.commonmark.node.BlockQuote
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.text.TextContentNodeRendererContext
import org.commonmark.renderer.text.TextContentRenderer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class ChannelPreview(
    val channel: Channel,
    socialStates: SocialStates,
    active: State<Boolean>,
    openMessageScreen: () -> Unit,
    openManagementDropdown: (position: ContextOptionMenu.Position, onClose: () -> Unit) -> Unit,
) : UIBlock(), SearchableItem {
    val otherUser: UUID? = channel.getOtherUser()

    private val messengerStates = socialStates.messages
    private val activityStates = socialStates.activity
    private val latestMessageState = messengerStates.getLatestMessage(channel.id)

    private val uuid = otherUser ?: UUID(0, 0)

    private val activity = activityStates.getActivityState(uuid)
    private val joinable = activity.map { it.isJoinable() }
    private val isOnline = memo { activity() !is PlayerActivity.Offline }

    val titleState = if (otherUser != null) {
        UuidNameLookup.nameState(otherUser)
    } else {
        messengerStates.getTitle(channel.id)
    }

    val hasUnreadState = messengerStates.getUnreadChannelState(channel.id)

    val isChannelMutedState = messengerStates.getMuted(channel.id)

    val isChannelSuspendedState = channel.getOtherUser()?.let { otherUser ->
        socialStates.isSuspended(otherUser)
    } ?: stateOf(false)

    val latestMessageTimestamp = latestMessageState.map { it?.createdAt ?: channel.joinedAt }

    private val doShowTimestampState = hasUnreadState.not() and latestMessageState.map { it != null }

    init {
        val dropdownOpen = mutableStateOf(false)
        val unreadQuantity = Tag(
            stateOf(EssentialPalette.RED),
            stateOf(EssentialPalette.TEXT_HIGHLIGHT),
            messengerStates.getNumUnread(channel.id).map { it.toString() },
        ) effect ShadowEffect(EssentialPalette.BLACK)
        val image = if (otherUser != null) {
            CachedAvatarImage.create(otherUser)
        } else {
            if (channel.isAnnouncement()) {
                EssentialPalette.ANNOUNCEMENT_ICON_8X.create()
            } else {
                EssentialPalette.groupIconForChannel(channel.id).create()
            }
        }
        val color = Modifier.whenTrue(
            hoveredState().toV2() or dropdownOpen or active,
            Modifier.color(EssentialPalette.COMPONENT_BACKGROUND),
            Modifier.color(EssentialPalette.GUI_BACKGROUND),
        )
        val timestampState = memo {
            val lastMessageDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(latestMessageTimestamp()), ZoneId.systemDefault())

            val lastMessageDate = lastMessageDateTime.toLocalDate()
            val currentDate = LocalDate.now()

            when (ChronoUnit.DAYS.between(lastMessageDate, currentDate)) {
                0L -> formatTime(lastMessageDateTime, false)
                1L -> "Yesterday"
                in 2L..7L -> lastMessageDate.format(weekdayTimestampFormatter)
                else -> formatDate(lastMessageDate, lastMessageDate.year != currentDate.year)
            }
        }

        layoutAsBox(Modifier.fillWidth().height(40f).then(BasicYModifier(::SiblingConstraint)).then(color)) {
            row(Modifier.fillParent()) {
                spacer(width = 6f)
                box(Modifier.width(32f).heightAspect(1f)) {
                    image(Modifier.width(24f).heightAspect(1f))
                }
                spacer(width = 3.5f)
                column(Modifier.fillRemainingWidth().fillHeight(), Arrangement.spacedBy(0f, FloatPosition.START), Alignment.Start) {
                    spacer(height = 10f)
                    row(Modifier.fillWidth()) {
                        box(Modifier.fillRemainingWidth()) {
                            text(titleState, truncateIfTooSmall = true, showTooltipForTruncatedText = false, modifier = Modifier.alignHorizontal(Alignment.Start))
                        }
                        if_(doShowTimestampState) {
                            spacer(width = 4f)
                            text(timestampState, shadow = false, modifier = Modifier.color(EssentialPalette.TEXT_DISABLED))
                        }
                    }
                    spacer(height = 5f)

                    fun LayoutScope.muteIndicator() {
                        row {
                            if_(isChannelMutedState and !hasUnreadState) {
                                image(EssentialPalette.MUTE_8X9, Modifier.color(EssentialPalette.TEXT_DISABLED))
                            }
                        }
                    }

                    if_(doShowTimestampState) {
                        row(Modifier.fillWidth(), Arrangement.SpaceBetween, Alignment.Start) {
                            description(Modifier.fillWidth(0.75f))
                            muteIndicator()
                        }
                    } `else` {
                        row(Modifier.fillWidth(), verticalAlignment = Alignment.Start) {
                            description(Modifier.fillRemainingWidth())
                            muteIndicator()
                        }
                    }
                }
                if_(hasUnreadState) {
                    spacer(width = 4f)
                    unreadQuantity(Modifier.childBasedWidth(padding = 2f).childBasedHeight(padding = 2f))
                }
                spacer(width = 10f)
            }
        }

        onRightClick {
            dropdownOpen.set(true)
            openManagementDropdown(ContextOptionMenu.Position(it.absoluteX, it.absoluteY)) {
                dropdownOpen.set(false)
            }
        }

        onLeftClick {
            USound.playButtonPress()
            openMessageScreen()
            it.stopPropagation()
        }
    }


    override fun getSearchTag() = titleState.get()

    companion object {
        private val markdownRenderer = TextContentRenderer.builder()
            .stripNewlines(false)
            .nodeRendererFactory(::PlainBlockQuoteNodeRenderer)
            .build()

        private val weekdayTimestampFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
    }

    /**
     * Renders BlockQuotes plainly instead of using guillemets like `CoreTextContentNodeRenderer`
     */
    class PlainBlockQuoteNodeRenderer(val context: TextContentNodeRendererContext) : NodeRenderer {
        private val content = context.writer

        override fun getNodeTypes(): Set<Class<out Node>> =
            setOf(BlockQuote::class.java)


        override fun render(node: Node) {
            content.write(">")
            visitChildren(node)

            if (node.next != null) {
                content.line()
            }
        }

        private fun visitChildren(parent: Node) {
            var node = parent.firstChild
            while (node != null) {
                context.render(node)
                node = node.next
            }
        }
    }

    private fun LayoutScope.description(modifier: Modifier) {
        box(modifier) {
            if_(joinable) {
                FriendStatus(uuid, activityStates)()
            } `else` {

                bind(latestMessageState) {

                    val (icon, text) = getDescriptionContent()
                    val descriptionModifier = Modifier.color(EssentialPalette.TEXT_DISABLED).alignHorizontal(Alignment.Start)

                    row(Modifier.fillWidth().alignHorizontal(Alignment.Start), Arrangement.spacedBy(5f), Alignment.End) {
                        if (icon != null) {
                            icon.create()(descriptionModifier)
                        }

                        box(Modifier.fillRemainingWidth()) {
                            text(
                                text.toV1(this@ChannelPreview),
                                descriptionModifier,
                                shadow = false,
                                truncateIfTooSmall = true,
                                showTooltipForTruncatedText = false,
                            )
                        }

                    }

                }

            }

        }

    }

    private fun getDescriptionContent(): Pair<ImageFactory?, State<String>> {
        val message = latestMessageState.get() ?: return Pair(
            null,
            stateOf(if (channel.isAnnouncement()) {
                "There are no announcements"
            } else {
                "Click to send a message!"
            })
        )
        return when (val part = message.parts.firstOrNull()) {
            is ClientMessage.Part.Text ->
                Pair(null, textDescription(part))
            is ClientMessage.Part.Image ->
                Pair(EssentialPalette.PICTURES_SHORT_9X7, State { pictureDescription(message) })
            is ClientMessage.Part.Skin ->
                Pair(EssentialPalette.PERSON_4X6, stateOf("Shared a skin"))
            is ClientMessage.Part.Gift ->
                Pair(EssentialPalette.WARDROBE_GIFT_7X, giftDescription(part.id))
            // is Outfit ->
            //    Pair(EssentialPalette.COSMETICS_10X7, stateOf("Shared an outfit")) // TODO: Add outfit message condition
            null ->
                Pair(null, stateOf("Unknown"))
        }
    }

    private fun textDescription(part: ClientMessage.Part.Text): State<String> {
        return memo {
            val text = if (EssentialConfig.chatFilterWithSource().first) part.filteredContent else part.unfilteredContent
            markdownRenderer.render(
                Parser.builder()
                    .build()
                    .parse(text)
            ).split("\n")[0] // stop at new line
        }
    }

    private fun Observer.pictureDescription(message: ClientMessage): String {
        var numberOfPictures = 0
        for (loopMessage in messengerStates.getMessageListState(channel.id)()
            .sortedByDescending { it.id }) {
            if (message.sender != loopMessage.sender) {
                break
            }
            if (loopMessage.content !is Media) {
                break
            }
            numberOfPictures += loopMessage.content.mediaIds.size
        }
        return "$numberOfPictures Picture" + (if (numberOfPictures == 1) "" else "s")
    }

    private fun giftDescription(cosmeticId: CosmeticId): State<String> {
        val cosmetic = platform.cosmeticsManager.cosmeticsData.getCosmetic(cosmeticId) ?: return stateOf("Gift")

        return stateOf("Gift: ${cosmetic.displayName}")
    }
}
