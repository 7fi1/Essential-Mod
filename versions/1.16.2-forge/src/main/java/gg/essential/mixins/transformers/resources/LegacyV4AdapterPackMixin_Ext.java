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
package gg.essential.mixins.transformers.resources;

import gg.essential.mixins.ext.client.resource.FileResourcePackExt;
import net.minecraft.client.resources.LegacyResourcePackWrapperV4;
import net.minecraft.resources.IResourcePack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;

@Mixin(LegacyResourcePackWrapperV4.class)
public class LegacyV4AdapterPackMixin_Ext implements FileResourcePackExt {

    @Shadow @Final private IResourcePack field_239479_h_;

    @Nullable
    @Override
    public Path getEssential$file() {
        if (this.field_239479_h_ instanceof FileResourcePackExt) {
            return ((FileResourcePackExt) this.field_239479_h_).getEssential$file();
        }
        return null;
    }
}
