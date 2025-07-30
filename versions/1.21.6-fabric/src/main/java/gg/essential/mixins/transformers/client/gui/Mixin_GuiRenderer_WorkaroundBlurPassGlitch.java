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

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.util.GuiRendererInfo;
import gg.essential.util.OwnedGlGpuTexture;
import gg.essential.util.UnownedGlGpuTexture;
import gg.essential.util.image.GpuTexture;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.texture.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// FIXME windows / nvidia GuiRenderer glitch.
// This is a workaround to prevent a rendering glitch that occurs on screens using a second GuiRenderer instance on some systems.
// The glitch presents itself as a big mess of pixels showing only the gui background all over a screen using a second GuiRenderer instance.
// One seemingly necessary condition is the depth texture clear call in [GuiRenderer.renderPreparedDraws], which occurs
// only when the gui has a blurred background layer.
// This workaround avoids the issue by copying a pre-cleared texture to the depth texture, we suspect the
// copy action alters the gpu scheduling in a way that avoids this glitch.
@Mixin(GuiRenderer.class)
public class Mixin_GuiRenderer_WorkaroundBlurPassGlitch {

    @Unique
    private OwnedGlGpuTexture clearTex = null;

    @Inject(method = "renderPreparedDraws", at = @At(value = "INVOKE",
            // right after the clear call
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearDepthTexture(Lcom/mojang/blaze3d/textures/GpuTexture;D)V",
            shift = At.Shift.AFTER))
    private void applyClearViaCopy(final CallbackInfo ci, @Local Framebuffer framebuffer) {
        if (GuiRendererInfo.INSTANCE.getCustomGuiRendererUsedThisFrame()) {
            GuiRendererInfo.INSTANCE.setCustomGuiRendererUsedThisFrame(false);

            var glTex = framebuffer.getDepthAttachment();
            if (!(glTex instanceof GlTexture)) return; // in case of third party mod changes

            var tex = new UnownedGlGpuTexture(
                    GpuTexture.Format.DEPTH32,
                    ((GlTexture) glTex).getGlId(),
                    glTex.getWidth(0),
                    glTex.getHeight(0)
            );

            // ensure clearTex is ready and clear
            if (clearTex == null) {
                clearTex = new OwnedGlGpuTexture(tex.getWidth(), tex.getHeight(), GpuTexture.Format.DEPTH32);
                clearTex.clearDepth(1f);
            } else if (clearTex.getWidth() != tex.getWidth() || clearTex.getHeight() != tex.getHeight()) {
                clearTex.resize(tex.getWidth(), tex.getHeight());
                clearTex.clearDepth(1f);
            }

            tex.copyFrom(clearTex);
        } else {
            cleanupClearTexture();
        }
    }

    @Unique
    private void cleanupClearTexture() {
        if (clearTex != null) {
            clearTex.delete();
            clearTex = null;
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void close(final CallbackInfo ci) {
        cleanupClearTexture();
    }
}
