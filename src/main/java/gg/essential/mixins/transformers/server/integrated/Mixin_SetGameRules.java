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
package gg.essential.mixins.transformers.server.integrated;

import gg.essential.mixins.ext.server.integrated.IntegratedServerExt;
import gg.essential.sps.McIntegratedServerManager;
import gg.essential.universal.UMinecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

//#if MC >= 1.21.11
//$$ import net.minecraft.world.rule.GameRule;
//$$ import org.jetbrains.annotations.Nullable;
//#endif

//#if MC >= 1.16.2
//$$ import net.minecraft.server.MinecraftServer;
//#endif

//#if MC < 1.21.11 && MC >= 1.16.2
//$$ @Mixin(GameRules.RuleValue.class)
//#else
@Mixin(GameRules.class)
//#endif
public abstract class Mixin_SetGameRules {

    //#if MC >= 1.21.11
    //$$ @Inject(method = "setValue", at = @At(value = "HEAD"))
    //$$ public <T> void onSetValue(GameRule<T> rule, T value, @Nullable MinecraftServer server, CallbackInfo ci) {
    //#elseif MC >= 1.16.2
    //$$ @Inject(method = "notifyChange", at = @At(value = "HEAD"))
    //$$ public void onNotifyChange(MinecraftServer server, CallbackInfo ci) {
    //#else
    @Inject(method = "setOrCreateGameRule", at = @At(value = "HEAD"))
    public void onSetOrCreateGameRule(String key, String ruleValue, CallbackInfo ci) {
        IntegratedServer server = UMinecraft.getMinecraft().getIntegratedServer();
    //#endif
        if (server == null || !server.isCallingFromMinecraftThread()) return;
        McIntegratedServerManager manager = ((IntegratedServerExt) server).getEssential$manager();
        if (manager.isGameRulesControlledByState()) {
            //#if MC >= 1.21.11
            //$$ manager.updateServerGameRules(Collections.singletonMap(rule.getId().toString(), value.toString()));
            //#elseif MC >= 1.16.2
            //$$ manager.updateServerGameRules();
            //#else
            manager.updateServerGameRules(Collections.singletonMap(key, ruleValue));
            //#endif
        }
    }

}
