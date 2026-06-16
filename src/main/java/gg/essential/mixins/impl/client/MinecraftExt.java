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
package gg.essential.mixins.impl.client;

import net.minecraft.util.Session;

//#if MC == 1.21.5
//$$ import net.minecraft.client.gl.Framebuffer;
//#endif

public interface MinecraftExt {
    /** Sets the private final session field and refreshes other objects that depend on the session. */
    void setSession(Session session);

    //#if MC == 1.21.5
    //$$ void essential$setFramebufferOverride(Framebuffer framebuffer);
    //#endif
}
