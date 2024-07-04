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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.essential.gui.emotes.EmoteWheel;
import gg.essential.universal.UMinecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Applies to <=1.12.2
@Mixin(GuiScreen.class)
public class Mixin_FixKeybindConsumedInEmoteWheel {

    @ModifyExpressionValue(method = "handleInput", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;isCreated()Z"))
    private boolean essential$shouldConsumeKeybind(boolean original) {
        if (UMinecraft.getMinecraft().currentScreen instanceof EmoteWheel) {
            return false;
        }
        return original;
    }
}
