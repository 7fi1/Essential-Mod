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
package gg.essential.mixins.transformers.feature.gl_debug;

import gg.essential.util.GlDebug;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 26.1
//$$ @Mixin(com.mojang.blaze3d.opengl.GlBackend.class)
//#else
@Mixin(net.minecraft.client.MainWindow.class)
//#endif
public class Mixin_EnableDebugContext {
    //#if MC >= 26.1
    //$$ @Inject(method = "setWindowHints", at = @At("TAIL"))
    //#else
    @Inject(method = "<init>", at = @At(value = "ESSENTIAL:AFTER_INVOKE_IN_INIT", target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V", remap = false))
    //#endif
    private void createDebugContext(CallbackInfo ci) {
        if (GlDebug.ENABLED) {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
        }
    }
}
