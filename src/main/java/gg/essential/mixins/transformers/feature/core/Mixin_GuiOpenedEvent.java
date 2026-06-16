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
package gg.essential.mixins.transformers.feature.core;

import gg.essential.Essential;
import gg.essential.event.gui.GuiOpenedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 26.2
//$$ @Mixin(net.minecraft.client.gui.Gui.class)
//#else
@Mixin(Minecraft.class)
//#endif
public class Mixin_GuiOpenedEvent {
    @Inject(method = "displayGuiScreen", at = @At("TAIL"))
    public void essential$fireGuiOpenedEvent(GuiScreen screen, CallbackInfo info) {
        if (screen == null) {
            return;
        }

        Essential.EVENT_BUS.post(new GuiOpenedEvent(screen));
    }
}
