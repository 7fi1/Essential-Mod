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

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import gg.essential.gui.overlay.OverlayManagerImpl;
import gg.essential.universal.UMouse;
import kotlin.Unit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Pseudo
@Mixin(AbstractElement.class) // https://github.com/Keksuccino/FancyMenu/tree/d30f69792a6e7d27c7e60e07971df6b99367d940/common/src/main/java/de/keksuccino/fancymenu/customization/element/AbstractElement.java
public class Mixin_AbstractElement_CacheRealMousePos {

    @Shadow(remap = false)
    protected int cachedMouseX; // Note: our mixin plugin automatically disables this mixin if this field isn't found. (e.g. pre 3.4.0 fancymenu)

    @Shadow(remap = false)
    protected int cachedMouseY;

    @Inject(method = "renderInternal", at = @At(value = "INVOKE",
            target = "Lde/keksuccino/fancymenu/customization/element/AbstractElement;tickBaseOpacity()V"),
            remap = false)
    private void cacheRealMousePos(CallbackInfo ci) {
        // No-op if we haven't overridden the mouse pos
        if (!OverlayManagerImpl.isOverridingMousePos()) return;

        OverlayManagerImpl.withRealMousePos(()-> {
            // Apply the real mouse pos to the cached values used by the parallax effect
            this.cachedMouseX = (int) UMouse.Scaled.getX();
            this.cachedMouseY = (int) UMouse.Scaled.getY();

            return Unit.INSTANCE;
        });
    }

}
