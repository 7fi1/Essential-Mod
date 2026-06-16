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
package gg.essential.mixins.transformers.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.systems.GpuSurface;
import gg.essential.Essential;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class Mixin_SuppressScreenshotBufferFlip {
    // On 26.2+, instead of suppressing the buffer swap, we'll suppress the acquisition of the next window surface
    // texture, MC will then not have a texture to blit to, and thereby skip the swap/present.
    @WrapWithCondition(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuSurface;acquireNextTexture()V"))
    private static boolean essential$suppressPresentOfScreenshotFrame(GpuSurface surface) {
        return !Essential.getInstance().getConnectionManager().getScreenshotManager().suppressBufferSwap();
    }
}
