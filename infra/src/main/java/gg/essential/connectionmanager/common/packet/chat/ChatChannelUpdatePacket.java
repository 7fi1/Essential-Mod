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
package gg.essential.connectionmanager.common.packet.chat;

import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.connectionmanager.common.packet.Packet;
import org.jetbrains.annotations.Nullable;

public class ChatChannelUpdatePacket extends Packet {

    @SerializedName("a")
    private final long channelId;

    @SerializedName("b")
    @Nullable
    private final String name;

    @SerializedName("c")
    @Nullable
    private final String topic;

    public ChatChannelUpdatePacket(final long channelId, @Nullable final String name, @Nullable final String topic) {
        this.channelId = channelId;
        this.name = name;
        this.topic = topic;
    }

    public long getChannelId() {
        return this.channelId;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    @Nullable
    public String getTopic() {
        return this.topic;
    }

}
