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
package gg.essential.connectionmanager.common.packet.cosmetic.categories;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.cosmetics.CosmeticSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ClientCosmeticCategoriesRequestPacket extends Packet {

    @SerializedName("a")
    private final @Nullable Set<@NotNull String> identifiers;

    @SerializedName("b")
    private final @Nullable Set<@NotNull CosmeticSlot> slots;

    @SerializedName("c")
    private final @Nullable Set<@NotNull String> tags;

    public ClientCosmeticCategoriesRequestPacket(
            final @Nullable Set<@NotNull String> identifiers,
            final @Nullable Set<@NotNull CosmeticSlot> slots,
            final @Nullable Set<@NotNull String> tags
    ) {
        this.identifiers = identifiers;
        this.slots = slots;
        this.tags = tags;
    }

    public @Nullable Set<@NotNull String> getIdentifiers() {
        return this.identifiers;
    }

    public @Nullable Set<@NotNull CosmeticSlot> getSlots() {
        return this.slots;
    }

    public @Nullable Set<@NotNull String> getTags() {
        return this.tags;
    }

}
