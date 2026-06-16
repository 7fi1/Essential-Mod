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
package gg.essential.mixins.transformers.server;

import net.minecraft.network.ServerStatusResponse;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor
    Thread getServerThread();

    //#if MC >= 1.19.4
    //$$ @Accessor
    //$$ void setFavicon(net.minecraft.server.ServerMetadata.Favicon favicon);
    //$$
    //$$ @Accessor
    //$$ void setMetadata(net.minecraft.server.ServerMetadata metadata);
    //$$
    //$$ @Invoker
    //$$ java.util.Optional<net.minecraft.server.ServerMetadata.Favicon> invokeLoadFavicon();
    //$$
    //$$ @Invoker
    //$$ net.minecraft.server.ServerMetadata invokeCreateMetadata();
    //#else
    @Invoker
    void invokeApplyServerIconToResponse(ServerStatusResponse statusResponse);
    //#endif

    //#if MC<11200
    //$$ @org.spongepowered.asm.mixin.gen.Invoker
    //$$ void invokeSaveAllWorlds(boolean dontLog);
    //#endif
}
