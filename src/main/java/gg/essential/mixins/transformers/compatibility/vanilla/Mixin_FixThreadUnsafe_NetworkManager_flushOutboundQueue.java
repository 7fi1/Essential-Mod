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
package gg.essential.mixins.transformers.compatibility.vanilla;

import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.locks.ReentrantReadWriteLock;

// `flushOutboundQueue` uses a `readLock` but then modifies the packet queue (by `poll`ing it).
// Two concurrent calls can therefore both see `queue.isEmpty()` as `false`, proceed to `poll` it, and have one of the
// two calls end up with `null`:
//
// [10:42:43] [Server Connector #1/ERROR] [minecraft/GuiConnecting]: Couldn't connect to server
// java.lang.NullPointerException: null
//         at net.minecraft.network.NetworkManager$InboundHandlerTuplePacketListener.access$100(NetworkManager.java:468) ~[gw$a.class:?]
//         at net.minecraft.network.NetworkManager.func_150733_h(NetworkManager.java:269) ~[gw.class:?]
//         at net.minecraft.network.NetworkManager.func_179290_a(NetworkManager.java:167) ~[gw.class:?]
//         at net.minecraft.client.multiplayer.GuiConnecting$1.run(GuiConnecting.java:71) [bkr$1.class:?
//
// This mixin fixes that by replacing the `readLock` with its `writeLock`.

//#if MC >= 1.16
//$$ // Minecraft now uses a regular `synchronized` block.
//$$ @org.spongepowered.asm.mixin.Mixin(gg.essential.mixins.DummyTarget.class)
//$$ public abstract class Mixin_FixThreadUnsafe_NetworkManager_flushOutboundQueue {}
//#else
@Mixin(NetworkManager.class)
public abstract class Mixin_FixThreadUnsafe_NetworkManager_flushOutboundQueue {
    @Shadow @Final private ReentrantReadWriteLock readWriteLock;

    @Redirect(method = "flushOutboundQueue", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantReadWriteLock$ReadLock;lock()V"))
    private void lockWriteLockInstead(ReentrantReadWriteLock.ReadLock readLock) {
        this.readWriteLock.writeLock().lock();
    }

    @Redirect(method = "flushOutboundQueue", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/locks/ReentrantReadWriteLock$ReadLock;unlock()V"))
    private void unlockWriteLockInstead(ReentrantReadWriteLock.ReadLock readLock) {
        this.readWriteLock.writeLock().unlock();
    }
}
//#endif
