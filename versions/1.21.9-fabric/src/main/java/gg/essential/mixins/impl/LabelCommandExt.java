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
package gg.essential.mixins.impl;

import gg.essential.cosmetics.CosmeticsRenderState;

//#if MC >= 26.2
//$$ import net.minecraft.client.renderer.feature.NameTagFeatureRenderer.Submit;
//#else
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl.LabelCommand;
//#endif

public interface LabelCommandExt {
    CosmeticsRenderState essential$getCosmeticsRenderState();

    static LabelCommandExt of(LabelCommand command) {
        return (LabelCommandExt) (Object) command;
    }
}
