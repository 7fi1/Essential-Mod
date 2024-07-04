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
package gg.essential.mixins.transformers.client;

import gg.essential.util.TargetThreadExecutor;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(Minecraft.class)
public class Mixin_RunEssentialTasks
    //#if MC>=11600
    //$$ extends Mixin_ThreadTaskExecutor
    //#endif
    implements gg.essential.mixins.ext.client.MinecraftExt {

    @Unique
    private final TargetThreadExecutor essentialExecutor = new TargetThreadExecutor();

    @NotNull
    @Override
    public Executor getEssential$executor() {
        return this.essentialExecutor;
    }

    //#if MC>=11600
    //$$ @Override
    //$$ public void runEssentialTasks(CallbackInfo callbackInfo) {
    //$$     this.essentialExecutor.run();
    //$$ }
    //#else
    @Inject(method = "runGameLoop", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", args = "ldc=scheduledExecutables", shift = At.Shift.AFTER))
    private void runEssentialExecutor(CallbackInfo ci) {
        this.essentialExecutor.run();
    }
    //#endif

    @Inject(
        //#if MC>=11600
        //$$ method = "<init>",
        //#else
        method = "init",
        //#endif
        at = @At(
            //#if MC<11400 || FABRIC
            value = "INVOKE",
            //#else
            //$$ value = "ESSENTIAL:INVOKE_IN_INIT",
            //#endif
            //#if MC>=11802
            //$$ target = "Lnet/minecraft/resource/ReloadableResourceManagerImpl;registerReloader(Lnet/minecraft/resource/ResourceReloader;)V"
            //#else
            target = "Lnet/minecraft/client/resources/IReloadableResourceManager;registerReloadListener(Lnet/minecraft/client/resources/IResourceManagerReloadListener;)V"
            //#endif
        )
    )
    private void runEssentialExecutorDuringInit(CallbackInfo ci) {
        this.essentialExecutor.run();
    }

}
