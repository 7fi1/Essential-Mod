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
package gg.essential.cosmetics.skinmask;

import gg.essential.lib.caffeine.cache.Cache;
import gg.essential.lib.caffeine.cache.Caffeine;
import gg.essential.lib.caffeine.cache.RemovalCause;
import gg.essential.lib.caffeine.cache.RemovalListener;
import gg.essential.lib.caffeine.cache.Scheduler;
import gg.essential.mixins.ext.client.renderer.PlayerSkinTextureExt;
import gg.essential.universal.UImage;
import gg.essential.universal.UMinecraft;
import gg.essential.util.ExtensionsKt;
import gg.essential.util.HelpersKt;
import gg.essential.util.Multithreading;
import gg.essential.util.image.bitmap.Bitmap;
import gg.essential.util.image.bitmap.UImageBitmap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static gg.essential.util.image.bitmap.GuiEssentialExtensionsKt.toUImage;

public class MaskedSkinProvider {
    private static final DynamicTextureManager dynamicTextureManager = new DynamicTextureManager();

    private ResourceLocation generatedSkin;
    private SkinMask generatedConfig;
    private ResourceLocation generatedId;

    public ResourceLocation provide(ResourceLocation skin, SkinMask config) {
        // Only need to change their skin if any of our cosmetics have a mask
        // and only if they have a custom skin (Steve/Alex do not have an outer layer)
        if (config.getParts().isEmpty() || !skin.getResourcePath().startsWith("skins/")) {
            generatedSkin = null;
            generatedConfig = null;
            return null;
        }

        final TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();

        // Check if we are already done
        if (skin.equals(generatedSkin) && config.equals(generatedConfig) && generatedId != null) {
            generatedConfig = config; // keep the specific object so the next `equals` can take the fast path
            dynamicTextureManager.keepAlive(this);
            return generatedId; // if so, we're good to go
        }

        // Need to generate a masked skin

        // Fetch the original skin
        ITextureObject skinTexture = textureManager.getTexture(skin);
        if (skinTexture == null) {
            return null; // if not, then we cannot yet apply the mask
        }

        // Sanity check, this should always be the case at least for vanilla
        if (!(skinTexture instanceof PlayerSkinTextureExt)) {
            return null; // this is bad, nothing we can do
        }
        PlayerSkinTextureExt skinTextureExt = (PlayerSkinTextureExt) skinTexture;

        // In most cases, the skin is only applied to the player once the texture has been fully downloaded.
        // If however the texture is currently downloading and it is requested a second time (e.g. when there are
        // player entities with the same skin), is however possible for the texture to be registered before it is
        // done downloading.
        UImage skinImage = skinTextureExt.essential$getImage();
        if (skinImage == null) {
            return null; // in that case, we cannot yet apply the mask
        }

        // All good, compute the masked skin, store it for later, and register it with MC
        Bitmap generatedTexture = config.apply(new UImageBitmap(skinImage));
        generatedSkin = skin;
        generatedConfig = config;
        generatedId = dynamicTextureManager.generateUniqueId(generatedSkin.toString().replace(':', '/'));
        dynamicTextureManager.register(this, generatedId, new MaskedSkinTexture(toUImage(generatedTexture)));
        return generatedId;
    }

    void expireTexture(ResourceLocation id) {
        if (id.equals(generatedId)) {
            generatedId = null;
        }
        Minecraft.getMinecraft().getTextureManager().deleteTexture(id);
    }

    private static class DynamicTextureManager implements RemovalListener<MaskedSkinProvider, ResourceLocation> {
        private final Cache<MaskedSkinProvider, ResourceLocation> loaded = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .executor(Multithreading.POOL)
            .scheduler(Scheduler.forScheduledExecutorService(Multithreading.getScheduledPool()))
            .removalListener(this)
            .build();

        private int nextUniqueId;

        public ResourceLocation generateUniqueId(String name) {
            return HelpersKt.identifier("essential", String.format(Locale.ROOT, "masked_skins/%s/%d", name, nextUniqueId++));
        }

        public void register(MaskedSkinProvider provider, ResourceLocation id, MaskedSkinTexture texture) {
            Minecraft.getMinecraft().getTextureManager().loadTexture(id, texture);
            loaded.put(provider, id);
        }

        public void keepAlive(MaskedSkinProvider provider) {
            loaded.getIfPresent(provider);
        }

        @Override
        public void onRemoval(@Nullable MaskedSkinProvider provider, @Nullable ResourceLocation id, @NotNull RemovalCause cause) {
            if (id == null || provider == null) {
                return;
            }
            ExtensionsKt.getExecutor(UMinecraft.getMinecraft()).execute(() -> provider.expireTexture(id));
        }
    }

    // Intentionally using a class which extends the vanilla skin texture class for better compatibility.
    private static class MaskedSkinTexture extends ThreadDownloadImageData {
        public MaskedSkinTexture(UImage image) {
            super(null, "essential-masked-image", DefaultPlayerSkin.getDefaultSkinLegacy(),
                //#if MC>=11600
                //$$ false,
                //#endif
                null);

            ((PlayerSkinTextureExt) this).essential$setImage(image);
        }

        @Override
        public void loadTexture(@NotNull IResourceManager resourceManager) {
        }

        //#if MC<11400
        @Override
        protected void loadTextureFromServer() {
        }
        //#endif
    }

}
