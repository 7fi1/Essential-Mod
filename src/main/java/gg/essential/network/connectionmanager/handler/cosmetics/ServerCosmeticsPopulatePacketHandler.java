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
package gg.essential.network.connectionmanager.handler.cosmetics;

import gg.essential.connectionmanager.common.packet.cosmetic.ServerCosmeticsPopulatePacket;
import gg.essential.cosmetics.model.Cosmetic;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsManager;
import gg.essential.network.connectionmanager.handler.PacketHandler;
import org.jetbrains.annotations.NotNull;

public class ServerCosmeticsPopulatePacketHandler extends PacketHandler<ServerCosmeticsPopulatePacket> {

    @Override
    protected void onHandle(@NotNull final ConnectionManager connectionManager, @NotNull final ServerCosmeticsPopulatePacket packet) {
        final CosmeticsManager cosmeticsManager = connectionManager.getCosmeticsManager();

        for (@NotNull final Cosmetic cosmetic : packet.getCosmetics()) {
            // FIXME these should not appear in production and are merely an artifact of improper migration
            //noinspection ConstantConditions
            if (cosmetic.getType() == null) {
                continue;
            }
            cosmeticsManager.getInfraCosmeticsData().addCosmetic(cosmetic);
        }
    }
}
