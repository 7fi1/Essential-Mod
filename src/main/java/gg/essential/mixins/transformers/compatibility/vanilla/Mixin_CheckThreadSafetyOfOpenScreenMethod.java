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

import gg.essential.Essential;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static gg.essential.universal.UMinecraft.isCallingFromMinecraftThread;

/**
 * Adds a sanity check to the start of `openScreen` which will log a warning when called
 * from a thread other than the client thread (because that's not safe to do!).
 */
@Mixin(
    //#if MC >= 26.2
    //$$ value = net.minecraft.client.gui.Gui.class,
    //#else
    value = Minecraft.class,
    //#endif
    // Extra low priority so we can log the issue before any other code potentially crashes the game
    priority = -50000
)
public class Mixin_CheckThreadSafetyOfOpenScreenMethod {
    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    private void checkThreadSafety(CallbackInfo ci) {
        if (!isCallingFromMinecraftThread()) {
            Essential.logger.error("Detected call to `openScreen` on thread {}. " +
                    "This method is NOT thread safe and MUST NOT be called from any thread except the main client thread! " +
                    "Please report this to the mod responsible as per the following stacktrace:",
                Thread.currentThread(),
                new Throwable());
        }
    }
}
