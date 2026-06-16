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
package gg.essential.mixins.transformers.feature.nameplate_icon;

import com.llamalad7.mixinextras.sugar.Local;
import gg.essential.handlers.OnlineIndicator;
import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 26.1
//$$ import net.minecraft.client.renderer.entity.EntityRenderer;
//$$ import net.minecraft.client.renderer.entity.state.EntityRenderState;
//#endif

//#if MC>=12109
//$$ import gg.essential.mixins.impl.client.model.PlayerEntityRenderStateExt;
//$$ import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
//#endif

//#if MC >= 26.1
//$$ @Mixin(EntityRenderer.class)
//#else
@Mixin(RenderPlayer.class)
//#endif
public class Mixin_NameplateIcon_PassExtraContext {
    //#if MC>=12109
    //#if MC >= 26.1
    //$$ private static final String METHOD = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V";
    //#else
    //$$ private static final String METHOD = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V";
    //#endif
    //#if MC >= 26.2
    //$$ private static final String TARGET = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V";
    //#else
    //$$ private static final String TARGET = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V";
    //#endif
    //#elseif MC>=12102
    //$$ private static final String METHOD = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";
    //$$ private static final String TARGET = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V";
    //#elseif MC>=12005
    //$$ private static final String METHOD = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V";
    //$$ private static final String TARGET = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;renderLabelIfPresent(Lnet/minecraft/entity/Entity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V";
    //#elseif MC>=11600
    //$$ private static final String METHOD = "renderName(Lnet/minecraft/client/entity/player/AbstractClientPlayerEntity;Lnet/minecraft/util/text/ITextComponent;Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V";
    //$$ private static final String TARGET = "Lnet/minecraft/client/renderer/entity/LivingRenderer;renderName(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/text/ITextComponent;Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;I)V";
    //#elseif MC>=11200
    private static final String METHOD = "renderEntityName";
    private static final String TARGET = "Lnet/minecraft/client/renderer/entity/RenderLivingBase;renderEntityName(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;D)V";
    //#else
    //$$ private static final String METHOD = "renderOffsetLivingLabel";
    //$$ private static final String TARGET = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderOffsetLivingLabel(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;FD)V";
    //#endif

    @Inject(
        method = METHOD,
        //#if MC>=11600
        //$$ at = @At(value = "INVOKE", target = TARGET, ordinal = 1)
        //#else
        at = @At(value = "INVOKE", target = TARGET)
        //#endif
    )
    private void passContext(
        CallbackInfo ci
        //#if MC >= 26.1
        //$$ , @Local(argsOnly = true) EntityRenderState state
        //#elseif MC>=12109
        //$$ , @Local(argsOnly = true) PlayerEntityRenderState state
        //#endif
        //#if MC==11202
        , @Local(argsOnly = true) AbstractClientPlayer entity
        //#endif
    ) {
        //#if MC >= 26.1
        //$$ if (!(state instanceof PlayerEntityRenderStateExt)) return;
        //#endif

        OnlineIndicator.currentlyDrawingPlayerEntityName.set(true);
        //#if MC>=12109
        //$$ OnlineIndicator.currentCosmeticsRenderState = ((PlayerEntityRenderStateExt) state).essential$getCosmetics();
        //#endif
        //#if MC==11202
        OnlineIndicator.nametagEntity = entity;
        //#endif
    }

    @Inject(
        method = METHOD,
        //#if MC>=11600
        //$$ at = @At(value = "INVOKE", target = TARGET, ordinal = 1, shift = At.Shift.AFTER)
        //#else
        at = @At(value = "INVOKE", target = TARGET, shift = At.Shift.AFTER)
        //#endif
    )
    private void resetContext(CallbackInfo ci) {
        OnlineIndicator.currentlyDrawingPlayerEntityName.set(false);
        //#if MC>=12109
        //$$ OnlineIndicator.currentCosmeticsRenderState = null;
        //#endif
        //#if MC==11202
        OnlineIndicator.nametagEntity = null;
        //#endif
    }
}
