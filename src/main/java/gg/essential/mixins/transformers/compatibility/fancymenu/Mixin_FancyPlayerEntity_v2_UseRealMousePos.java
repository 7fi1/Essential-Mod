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
package gg.essential.mixins.transformers.compatibility.fancymenu;

import org.spongepowered.asm.mixin.Mixin;

//#if MC < 1.20.5
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gg.essential.gui.overlay.OverlayManagerImpl;
import kotlin.Unit;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Pseudo;

// this targets the old v2 versions of fancy menu, with the player element there needing the unmodified mouse position
@Pseudo
@Mixin(targets = {
        // The exact versions for these differences vary a little between loaders, presumably due to backporting / LTS
        // 1.18 -> 1.20.4 @v2
        // https://github.com/Keksuccino/FancyMenu/blob/63d3ede2c140b3ae64600247be2b54219b2f7799/src/main/java/de/keksuccino/fancymenu/menu/fancy/item/items/playerentity/PlayerEntityCustomizationItem.java
        "de.keksuccino.fancymenu.menu.fancy.item.items.playerentity.PlayerEntityCustomizationItem",
        // 1.12 -> 1.19.2 @v2
        // https://github.com/Keksuccino/FancyMenu/blob/4b85ac0906d8b1862312779d1efe5ec48b8dec31/src/main/java/de/keksuccino/fancymenu/menu/fancy/item/playerentity/PlayerEntityCustomizationItem.java
        "de.keksuccino.fancymenu.menu.fancy.item.playerentity.PlayerEntityCustomizationItem"
})
//#else
//$$ import gg.essential.mixins.DummyTarget;
//$$ @Mixin(DummyTarget.class)
//#endif
public class Mixin_FancyPlayerEntity_v2_UseRealMousePos {

    //#if MC < 1.20.5
    // Mouse position gets retrieved multiple times during this render, best to just wrap it all with the global mouse pos restored
    @WrapMethod(method = "render")
    private void useRealMousePosV2(
            //#if MC >= 1.20
            //$$ net.minecraft.client.gui.DrawContext param0,
            //#elseif MC >= 1.16
            //$$ com.mojang.blaze3d.matrix.MatrixStack param0,
            //#endif
            GuiScreen param, Operation<Void> original) {

        if (!OverlayManagerImpl.isOverridingMousePos()) {
            original.call(
                    //#if MC >= 1.16
                    //$$ param0,
                    //#endif
                    param
            );
            return;
        }

        OverlayManagerImpl.withRealMousePos(()-> {
            original.call(
                    //#if MC >= 1.16
                    //$$ param0,
                    //#endif
                    param
            );
            return Unit.INSTANCE;
        });
    }
    //#endif

}
