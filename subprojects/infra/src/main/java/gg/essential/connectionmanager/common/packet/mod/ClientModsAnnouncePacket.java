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
package gg.essential.connectionmanager.common.packet.mod;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ClientModsAnnouncePacket extends Packet {

    @NotNull
    @SerializedName("b")
    private final String minecraftVersion;

    @NotNull
    @SerializedName("c")
    private final Platform platform;

    @NotNull
    @SerializedName("d")
    private final String platformVersion;

    private final @Nullable String modpackId;

    public ClientModsAnnouncePacket(
            final @NotNull String minecraftVersion,
            final @NotNull Platform platform,
            final @NotNull String platformVersion,
            final @Nullable String modpackId
    ) {
        this.minecraftVersion = minecraftVersion;
        this.platform = platform;
        this.platformVersion = platformVersion;
        this.modpackId = modpackId;
    }

    @NotNull
    public String getMinecraftVersion() {
        return this.minecraftVersion;
    }

    @NotNull
    public Platform getPlatform() {
        return this.platform;
    }

    @NotNull
    public String getPlatformVersion() {
        return this.platformVersion;
    }

    public @Nullable String getModpackId() {
        return this.modpackId;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ClientModsAnnouncePacket that = (ClientModsAnnouncePacket) o;
        return this.minecraftVersion.equals(that.minecraftVersion)
                && this.platform == that.platform
                && this.platformVersion.equals(that.platformVersion)
                && Objects.equals(this.modpackId, that.modpackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.minecraftVersion, this.platform, this.platformVersion) * 31;
    }

    @Override
    public String toString() {
        return "ModsAnnouncePacket{" +
                "minecraftVersion=" + this.minecraftVersion +
                ", platform=" + this.platform +
                ", platformVersion='" + this.platformVersion + '\'' +
                ", modpackId='" + this.modpackId + '\'' +
                '}';
    }

    public static enum Platform {

        FORGE, FABRIC, NEOFORGE

    }

}
