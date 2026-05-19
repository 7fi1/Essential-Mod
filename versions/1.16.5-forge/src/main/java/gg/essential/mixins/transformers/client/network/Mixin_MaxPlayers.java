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
package gg.essential.mixins.transformers.client.network;

import gg.essential.mixins.ext.client.network.NetHandlerPlayClientExt;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.play.server.SJoinGamePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetHandler.class)
public abstract class Mixin_MaxPlayers implements NetHandlerPlayClientExt {
    @Unique
    private int essential$maxPlayers = -1;

    @Inject(method = "handleJoinGame", at = @At("TAIL"))
    private void captureMaxPlayers(SJoinGamePacket packetIn, CallbackInfo ci) {
        //#if MC >= 1.18.1
        //$$ this.essential$maxPlayers = packetIn.maxPlayers();
        //#else
        this.essential$maxPlayers = ((SJoinGamePacketAccessor) packetIn).getMaxPlayers();
        //#endif
    }

    @Override
    public int getEssential$maxPlayers() {
        return this.essential$maxPlayers;
    }
}
