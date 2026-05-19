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
package gg.essential.mixins.transformers.compatibility.vanilla;

import gg.essential.util.HelpersKt;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

//#if MC < 1.16
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
//#endif

//#if MC < 1.12
//$$ import net.minecraft.client.resources.I18n;
//#endif

//#if MC >= 1.16
//$$ @Mixin(net.minecraft.server.integrated.IntegratedServer.class)
//#else
@Mixin(targets = "net.minecraft.server.integrated.IntegratedServer$3")
//#endif
public class Mixin_DisconnectPlayersOnServerClose {
    //#if MC >= 1.16
    //$$ @Inject(method = {"func_210176_c",
    //#if FORGE
    //$$ // Optifine changes the name of the lambda with its patches
    //#if MC >= 1.19.3
    //$$ "lambda$initiateShutdown$3",
    //#elseif MC >= 1.19.2
    //$$ "lambda$initiateShutdown$1",
    //#elseif MC >= 1.18
    //$$ "lambda$initiateShutdown$2",
    //#elseif MC >= 1.16
    //$$ "lambda$initiateShutdown$1",
    //#endif
    //#endif
    //$$ }, at = @At("HEAD"))
    //#else
    @Inject(method = "run", at = @At("HEAD"), remap = false)
    //#endif
    public void disconnectPlayers(CallbackInfo ci) {
        //#if MC >= 1.16
        //$$ MinecraftServer server = (MinecraftServer) (Object) this;
        //#else
        MinecraftServer server = this.this$0;
        //#endif

        for (EntityPlayerMP player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            //#if MC >= 1.19
            //$$ if (player.getUuid().equals(server.getHostProfile().getId())) continue;
            //#else
            if (player.getName().equals(server.getServerOwner())) continue;
            //#endif

            //#if MC >= 1.12
            player.connection.disconnect(
                    HelpersKt.textTranslatable("multiplayer.disconnect.server_shutdown")
            );
            //#else
            //$$ player.playerNetServerHandler.kickPlayerFromServer(
            //$$     I18n.format("multiplayer.disconnect.server_shutdown")
            //$$ );
            //#endif
        }
    }

    //#if MC < 1.16
    @Shadow @Final IntegratedServer this$0;
    //#endif
}
