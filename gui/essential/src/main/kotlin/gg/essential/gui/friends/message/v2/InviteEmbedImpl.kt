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

import com.sparkuniverse.toolbox.chat.model.InviteMessageContent
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.width
import gg.essential.gui.EssentialPalette
import gg.essential.gui.common.EssentialTooltip
import gg.essential.gui.common.LoadingIcon
import gg.essential.gui.elementa.state.v2.State
import gg.essential.gui.elementa.state.v2.asyncMap
import gg.essential.gui.elementa.state.v2.memo
import gg.essential.gui.elementa.state.v2.toListState
import gg.essential.gui.elementa.state.v2.toV1
import gg.essential.gui.image.ImageFactory
import gg.essential.gui.layoutdsl.*
import gg.essential.gui.util.hoveredStateV2
import gg.essential.gui.util.pollingStateV2
import gg.essential.sps.SpsAddress
import gg.essential.sps.toMod
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import gg.essential.util.ServerPingInfoState
import gg.essential.util.ServerType
import gg.essential.util.UuidNameLookup
import gg.essential.util.loadUIImage
import gg.essential.util.onRightClick
import gg.essential.util.toImageFactory
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Color
import java.awt.color.ColorSpace
import java.awt.image.ColorConvertOp
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

class InviteEmbedImpl(
    private val invite: InviteMessageContent,
    messageWrapper: MessageWrapper,
) : InviteEmbed(messageWrapper) {

    private val address = when (invite) {
        is InviteMessageContent.ISPSInvite -> SpsAddress(invite.host).toString()
        is InviteMessageContent.IServerInvite -> invite.address
    }

    private val displayName = memo {
        when (invite) {
            is InviteMessageContent.ISPSInvite -> invite.worldName ?: run {
                val username = UuidNameLookup.nameState(invite.host)()
                val formattedUsername = if (username.isNotBlank()) "$username's " else ""
                "${formattedUsername}World"
            }
            is InviteMessageContent.IServerInvite -> invite.address
        }
    }

    private val modLoader = when (invite) {
        is InviteMessageContent.ISPSInvite -> invite.modLoader?.toMod()
        is InviteMessageContent.IServerInvite -> null
    }

    private val serverPingInfoState = ServerPingInfoState.forServer(address)
    private val serverIconState: State<ImageFactory?> = memo {
        val shouldDisplayIcon = when (invite) {
            // For disabled SPS worlds, do not display any icon. Showing the active world's icon would be incorrect.
            is InviteMessageContent.ISPSInvite -> !isDisabled()
            is InviteMessageContent.IServerInvite -> true
        }
        val icon = if (shouldDisplayIcon) serverPingInfoState()?.iconImage else null
        icon?.let {
            if (isDisabled()) {
                ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(it, null)
            } else {
                it
            }
        }
    }.asyncMap(GlobalScope) {
        if (it == null) return@asyncMap EssentialPalette.DEFAULT_WORLD_ICON_64X
        loadUIImage(it).toImageFactory()
    }
    private val isDisabled: State<Boolean> = when (invite) {
        is InviteMessageContent.ISPSInvite -> {
            val isValid = pollingStateV2 { platform.haveActiveRemoteSpsSession(invite.host) }
            memo { invite.expired || serverPingInfoState() == null || !isValid() }
        }
        is InviteMessageContent.IServerInvite -> memo { serverPingInfoState() == null }
    }
    private val hasAlreadyJoined: State<Boolean> = pollingStateV2 {
        when (val currentServer = ServerType.current()) {
            is ServerType.Multiplayer -> currentServer.address == address
            is ServerType.SPS -> when (invite) {
                is InviteMessageContent.ISPSInvite -> currentServer.hostUuid == invite.host
                is InviteMessageContent.IServerInvite -> false
            }
            else -> false
        }
    }
    private val joinButtonInfo: State<Pair</*disabled*/Boolean, /*tooltip*/String>> = memo {
        when {
            isDisabled() -> when (invite) {
                is InviteMessageContent.ISPSInvite -> true to "World invite expired"
                is InviteMessageContent.IServerInvite -> true to "Server is offline"
            }
            hasAlreadyJoined() -> true to "Already joined"
            else -> false to ""
        }
    }

    init {
        if (invite !is InviteMessageContent.ISPSInvite || !invite.expired) {
            var coroutineScope: CoroutineScope? = null
            var timeoutJob: Job? = null
            addUpdateFunc { _, _ ->
                if (coroutineScope?.isActive != true) {
                    coroutineScope = CoroutineScope(Job())
                    coroutineScope.launch { serverPingInfoState.refresh() }
                }
                timeoutJob?.cancel()
                timeoutJob = coroutineScope.launch {
                    kotlinx.coroutines.delay(10.seconds)
                    coroutineScope.cancel()
                }
            }
        }

        val hoverState = hoveredStateV2()
        colorState.rebind(memo {
            if (hoverState()) EssentialPalette.GRAY_BUTTON_HOVER else EssentialPalette.GRAY_BUTTON
        }.toV1(this))

        constrain {
            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }

        bubble.layoutAsBox(Modifier.hoverScope()) {
            column(Modifier.childBasedHeight(1f)) {
                content()
            }
        }.onRightClick {
            messageWrapper.openOptionMenu(it, this@InviteEmbedImpl)
        }
    }

    private fun LayoutScope.content() {
        val description = when (invite) {
            is InviteMessageContent.ISPSInvite -> memo {
                if (isDisabled()) return@memo listOf("Invite Expired")
                val serverInfo = serverPingInfoState() ?: return@memo emptyList<String>()
                listOf(
                    "${serverInfo.version} ${modLoader?.name ?: ""}",
                    "${serverInfo.onlinePlayers}/${serverInfo.maxPlayers} Players",
                )
            }
            is InviteMessageContent.IServerInvite -> memo {
                if (isDisabled()) return@memo listOf("Offline")
                val serverInfo = serverPingInfoState() ?: return@memo emptyList<String>()
                listOf(
                    "${serverInfo.onlinePlayers} Player${if (serverInfo.onlinePlayers != 1) "s" else ""}"
                )
            }
        }.toListState()

        val contentWidth = memo {
            val maxDescriptionWidth = description().maxOfOrNull { it.width() } ?: 0f
            max(maxDescriptionWidth, displayName().width()).coerceIn(73f, 108f)
        }
        row {
            serverIcon()
            spacer(width = 10f)
            column(
                Modifier.then(State { Modifier.width(contentWidth()) }),
                Arrangement.spacedBy(4f),
                Alignment.Start
            ) {
                text(
                    displayName,
                    Modifier.shadow(Color.BLACK),
                    truncateIfTooSmall = true
                )
                forEach(description) { line ->
                    text(
                        line,
                        Modifier.color(EssentialPalette.TEXT_MID_GRAY).shadow(Color.BLACK),
                        truncateIfTooSmall = true
                    )
                }
            }
            spacer(width = 15f)
            joinButton()
        }
    }

    private fun LayoutScope.serverIcon() {
        val iconModifier = Modifier.width(32f).heightAspect(1f).shadow(Color.BLACK)
        bind(serverIconState) { icon ->
            icon?.let { it.create()(iconModifier) } ?: LoadingIcon(2.0)(iconModifier)
        }
    }

    private fun LayoutScope.joinButton() {
        val buttonModifier = Modifier.childBasedWidth(10f).childBasedHeight(4f)
            .shadow(Color.BLACK).then(State {
                if (joinButtonInfo().first) {
                    Modifier.color(EssentialPalette.BLUE_BUTTON_DISABLED)
                        .hoverTooltip(joinButtonInfo().second, position = EssentialTooltip.Position.ABOVE)
                } else {
                    Modifier.color(EssentialPalette.BLUE_BUTTON).hoverColor(EssentialPalette.BLUE_BUTTON_HOVER)
                }
            }).hoverScope()
        val textModifier = Modifier.shadow(EssentialPalette.TEXT_SHADOW).color {
            if (joinButtonInfo().first) EssentialPalette.TEXT_DISABLED else EssentialPalette.TEXT_HIGHLIGHT
        }

        column(buttonModifier) {
            spacer(height = 1f) // Extra pixel for text shadow
            text("Join", textModifier)
        }.onLeftClick {
            if (!joinButtonInfo.getUntracked().first) {
                platform.connectToServer(displayName.getUntracked(), address)
            }
        }
    }

    override fun beginHighlight() {}

    override fun releaseHighlight() {}

}