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
package com.sparkuniverse.toolbox.chat.model;

import gg.essential.lib.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Message {

    @SerializedName("a")
    private final long id;

    @SerializedName("b")
    private final long channelId;

    @SerializedName("c")
    @NotNull
    private final UUID sender;

    // d: String contents (replaced with `MessageContent` "content" in protocol 10)

    @SerializedName("content")
    @NotNull
    public MessageContent content;

    @Deprecated
    @SerializedName("e")
    private final boolean read;

    @SerializedName("f")
    @Nullable
    private final Long replyTargetId;

    @SerializedName("g")
    @Nullable
    private final Long lastEditTime;

    @SerializedName("created_at")
    public final long createdAt;

    // unfiltered_contents: String (replaced with `MessageContent` "content" in protocol 10)

    public Message(
            final long id,
            final long channelId,
            final @NotNull UUID sender,
            final @NotNull MessageContent content,
            final boolean read,
            final @Nullable Long replyTargetId,
            final @Nullable Long lastEditTime,
            final long createdAt
    ) {
        this.id = id;
        this.channelId = channelId;
        this.sender = sender;
        this.content = content;
        this.read = read;
        this.replyTargetId = replyTargetId;
        this.lastEditTime = lastEditTime;
        this.createdAt = createdAt;
    }

    public long getId() {
        return this.id;
    }

    public long getChannelId() {
        return this.channelId;
    }

    @NotNull
    public UUID getSender() {
        return this.sender;
    }

    @NotNull
    public MessageContent getContent() {
        return this.content;
    }

    @Deprecated
    public boolean isRead() {
        return this.read;
    }

    @Nullable
    public Long getReplyTargetId() {
        return replyTargetId;
    }

    @Nullable
    public Long getLastEditTime() {
        return lastEditTime;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public boolean equals(final @Nullable Object object) {
        if (this == object) return true;
        if (object == null || this.getClass() != object.getClass()) return false;

        final Message message = (Message) object;

        return id == message.id && channelId == message.channelId;
    }

    @Override
    public int hashCode() {
        return 31 * (217 + Long.hashCode(id)) + Long.hashCode(channelId);
    }

}
