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
package gg.essential.util

import gg.essential.Essential
import gg.essential.config.AccessedViaReflection
import gg.essential.config.EssentialConfig
import gg.essential.elementa.components.Window
import gg.essential.event.client.ReAuthEvent
import gg.essential.gui.common.modal.Modal
import gg.essential.gui.elementa.essentialmarkdown.EssentialMarkdown
import gg.essential.gui.elementa.state.v2.MutableState
import gg.essential.gui.friends.message.v2.MessageRef
import gg.essential.gui.friends.state.IMessengerStates
import gg.essential.gui.friends.state.IRelationshipStates
import gg.essential.gui.friends.state.IStatusStates
import gg.essential.gui.friends.state.MessengerStateManagerImpl
import gg.essential.gui.friends.state.RelationshipStateManagerImpl
import gg.essential.gui.friends.state.SocialStates
import gg.essential.gui.friends.state.StatusStateManagerImpl
import gg.essential.gui.notification.NotificationsImpl
import gg.essential.gui.notification.NotificationsManager
import gg.essential.gui.overlay.ModalManager
import gg.essential.gui.overlay.ModalManagerImpl
import gg.essential.gui.overlay.OverlayManager
import gg.essential.gui.overlay.OverlayManagerImpl
import gg.essential.gui.screenshot.bytebuf.LimitedAllocator
import gg.essential.gui.util.onAnimationFrame
import gg.essential.handlers.EssentialSoundManager
import gg.essential.handlers.PauseMenuDisplay
import gg.essential.model.backend.RenderBackend
import gg.essential.model.backend.minecraft.MinecraftRenderBackend
import gg.essential.model.util.Color
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import gg.essential.network.connectionmanager.media.IScreenshotManager
import gg.essential.network.connectionmanager.notices.INoticesManager
import gg.essential.universal.UGraphics
import gg.essential.universal.UImage
import gg.essential.universal.UScreen
import gg.essential.universal.utils.ReleasedDynamicTexture
import gg.essential.util.image.bitmap.Bitmap
import gg.essential.util.image.bitmap.MutableBitmap
import gg.essential.util.image.bitmap.forEachPixel
import gg.essential.util.lwjgl3.Lwjgl3Loader
import io.netty.buffer.ByteBuf
import kotlinx.coroutines.CoroutineDispatcher
import me.kbrewster.eventbus.Subscribe
import net.minecraft.client.Minecraft
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import kotlin.jvm.Throws

@AccessedViaReflection("GuiEssentialPlatform")
class GuiEssentialPlatformImpl : GuiEssentialPlatform {
    override val clientThreadDispatcher: CoroutineDispatcher
        get() = MinecraftCoroutineDispatchers.clientThread

    override val renderThreadDispatcher: CoroutineDispatcher
        get() = MinecraftCoroutineDispatchers.renderThread

    override val renderBackend: RenderBackend
        get() = MinecraftRenderBackend

    override val overlayManager: OverlayManager
        get() = OverlayManagerImpl

    override val assetLoader: AssetLoader
        get() = Essential.getInstance().connectionManager.cosmeticsManager.assetLoader

    override val lwjgl3: Lwjgl3Loader
        get() = Essential.getInstance().lwjgl3

    override val cmConnection: CMConnection
        get() = Essential.getInstance().connectionManager

    override val notifications: NotificationsManager
        get() = NotificationsImpl

    override fun createModalManager(): ModalManager {
        return ModalManagerImpl(OverlayManagerImpl)
    }

    override fun onResourceManagerReload(runnable: Runnable) {
        ResourceManagerUtil.onResourceManagerReload { runnable.run() }
    }

    @Throws(IOException::class)
    override fun bitmapFromMinecraftResource(identifier: UIdentifier): MutableBitmap? {
        val resource = ResourceManagerUtil.getResource(identifier.toMC()) ?: return null
        resource.inputStream.use {
            return bitmapFromInputStream(it)
        }
    }

    @Throws(IOException::class)
    override fun bitmapFromInputStream(inputStream: InputStream): MutableBitmap {
        val image = UImage.read(inputStream)
        val bitmap = Bitmap.ofSize(image.getWidth(), image.getHeight())
        bitmap.forEachPixel { _, x, y ->
            bitmap[x, y] = Color(image.getPixelRGBA(x, y).toUInt())
        }
        //#if MC>=11600
        //$$ image.nativeImage.close()
        //#endif
        return bitmap
    }

    override fun uImageIntoReleasedDynamicTexture(uImage: UImage): ReleasedDynamicTexture {
        return ReleasedDynamicTexture(uImage.nativeImage)
    }

    override fun bindTexture(textureUnit: Int, identifier: UIdentifier) {
        UGraphics.bindTexture(textureUnit, identifier.toMC())
    }

    override fun playSound(identifier: UIdentifier) {
        EssentialSoundManager.playSound(identifier.toMC())
    }

    override fun registerCosmeticTexture(name: String, texture: ReleasedDynamicTexture): UIdentifier {
        return MinecraftRenderBackend.CosmeticTexture(name, texture).identifier.toU()
    }

    override fun dismissModalOnScreenChange(modal: Modal, dismiss: () -> Unit) {
        var screen = UScreen.currentScreen
        modal.onAnimationFrame {
            val newScreen = UScreen.currentScreen
            if (newScreen == null || newScreen == screen || newScreen is OverlayManagerImpl.OverlayInteractionScreen)
                return@onAnimationFrame

            screen = UScreen.currentScreen
            Window.enqueueRenderOperation {
                dismiss()
            }
        }
    }

    override val essentialBaseDir: Path
        get() = Essential.getInstance().baseDir.toPath()

    override val config: GuiEssentialPlatform.Config
        get() = EssentialConfig

    override val pauseMenuDisplayWindow: Window
        get() = PauseMenuDisplay.window

    override val mcProtocolVersion: Int
        get() = MinecraftUtils.currentProtocolVersion

    override val mcGameVersion: String
        //#if MC>=11600
        //$$ get() = net.minecraft.util.SharedConstants.getVersion().name
        //#elseif MC>=11200
        get() = "1.12.2"
        //#else
        //$$ get() = "1.8.9"
        //#endif

    override fun currentServerType(): ServerType? {
        val minecraft = Minecraft.getMinecraft()
        val spsManager = Essential.getInstance().connectionManager.spsManager

        val localSpsSession = spsManager.localSession
        if (localSpsSession != null) {
            return ServerType.SPS.Host(localSpsSession.hostUUID)
        }

        if (minecraft.isSingleplayer) {
            return ServerType.Singleplayer
        }

        //#if MC<12002
        if (minecraft.isConnectedToRealms) {
            return ServerType.Realms
        }
        //#endif

        val serverData = minecraft.currentServerData ?: return null

        //#if MC>=12002
        //$$ if (serverData.isRealm) {
        //$$     return ServerType.Realms
        //$$ }
        //#endif

        val remoteSpsHost = spsManager.getHostFromSpsAddress(serverData.serverIP)
        if (remoteSpsHost != null) {
            return ServerType.SPS.Guest(remoteSpsHost)
        }

        return ServerType.Multiplayer(serverData.serverName, serverData.serverIP)
    }

    override fun registerActiveSessionState(state: MutableState<USession>) {
        state.set(Minecraft.getMinecraft().session.toUSession())
        Essential.EVENT_BUS.register(object {
            @Subscribe(priority = 1000)
            private fun onReAuth(event: ReAuthEvent) {
                state.set(event.session)
            }
        })
    }

    override fun createSocialStates(): SocialStates {
        val cm = Essential.getInstance().connectionManager
        return object : SocialStates {
            override val relationships: IRelationshipStates by lazy { RelationshipStateManagerImpl(cm.relationshipManager) }
            override val messages: IMessengerStates by lazy { MessengerStateManagerImpl(cm.chatManager) }
            override val activity: IStatusStates by lazy { StatusStateManagerImpl(cm.profileManager, cm.spsManager) }
        }
    }

    override fun resolveMessageRef(messageRef: MessageRef) {
        Essential.getInstance().connectionManager.chatManager.retrieveChannelHistoryUntil(messageRef)
    }

    override val essentialUriListener: EssentialMarkdown.(EssentialMarkdown.LinkClickEvent) -> Unit
        get() = gg.essential.util.essentialUriListener

    override val noticesManager: INoticesManager
        get() = Essential.getInstance().connectionManager.noticesManager

    override val screenshotManager: IScreenshotManager
        get() = Essential.getInstance().connectionManager.screenshotManager

    override val isOptiFineInstalled: Boolean
        get() = OptiFineUtil.isLoaded()

    override fun trackByteBuf(alloc: LimitedAllocator, buf: ByteBuf): ByteBuf =
        gg.essential.gui.screenshot.bytebuf.trackByteBuf(alloc, buf)
}