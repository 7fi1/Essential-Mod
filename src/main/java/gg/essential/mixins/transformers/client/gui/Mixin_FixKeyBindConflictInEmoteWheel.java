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
package gg.essential.mixins.transformers.client.gui;

import gg.essential.gui.emotes.EmoteWheel;
import gg.essential.universal.UScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Applies to 1.12.2+ Forge
//#if MC >= 11202 && FORGE
@Mixin(targets = "net.minecraftforge.client.settings.KeyConflictContext$2", remap = false)
//#else
//$$ @Mixin(gg.essential.mixins.DummyTarget.class)
//#endif
public class Mixin_FixKeyBindConflictInEmoteWheel {

    //#if FORGE
    @Inject(method = "isActive()Z", at = @At("HEAD"), cancellable = true, remap = false)
    public void essential$onGetActive(CallbackInfoReturnable<Boolean> cir) {
        if (UScreen.getCurrentScreen() instanceof EmoteWheel) {
            cir.setReturnValue(false);
        }
    }
    //#endif
}
