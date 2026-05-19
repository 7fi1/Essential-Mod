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

import gg.essential.lib.gson.JsonDeserializationContext;
import gg.essential.lib.gson.JsonDeserializer;
import gg.essential.lib.gson.JsonElement;
import gg.essential.lib.gson.JsonObject;
import gg.essential.lib.gson.JsonSerializationContext;
import gg.essential.lib.gson.JsonSerializer;
import gg.essential.lib.gson.annotations.JsonAdapter;
import gg.essential.lib.gson.annotations.SerializedName;
import gg.essential.skins.SkinModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Set;

@JsonAdapter(MessageContent.Serializer.class)
public class MessageContent {
    private final MessageContentType type;

    protected MessageContent(MessageContentType type) {
        this.type = type;
    }

    public MessageContentType getType() {
        return type;
    }

    public static final class Plain extends MessageContent {
        private final @NotNull String text;
        private final @Nullable String unfiltered;

        public Plain(final @NotNull String text, final @Nullable String unfiltered) {
            super(MessageContentType.PLAIN);

            this.text = text;
            this.unfiltered = unfiltered;
        }

        public @NotNull String getUnfilteredText() {
            if (unfiltered != null) {
                return unfiltered;
            } else {
                return text;
            }
        }

        public @NotNull String getText() {
            return text;
        }
    }

    public static final class Media extends MessageContent {
        @SerializedName("media_ids")
        private final @NotNull Set<String> mediaIds;

        public Media(final @NotNull Set<String> mediaIds) {
            super(MessageContentType.MEDIA);

            this.mediaIds = mediaIds;
        }

        public @NotNull Set<String> getMediaIds() {
            return mediaIds;
        }
    }

    public static final class CosmeticGift extends MessageContent {
        @SerializedName("cosmetic_id")
        private final @NotNull String cosmeticId;

        public CosmeticGift(final @NotNull String cosmeticId) {
            super(MessageContentType.COSMETIC_GIFT);

            this.cosmeticId = cosmeticId;
        }

        public @NotNull String getCosmeticId() {
            return cosmeticId;
        }
    }

    public static final class Skin extends MessageContent {
        private final @NotNull String hash;
        private final @NotNull SkinModel model;

        public Skin(final @NotNull String hash, final @NotNull SkinModel model) {
            super(MessageContentType.SKIN);

            this.hash = hash;
            this.model = model;
        }

        public @NotNull String getHash() {
            return hash;
        }

        public @NotNull SkinModel getModel() {
            return model;
        }
    }

    public static final class Unknown extends MessageContent {
        public Unknown() {
            super(MessageContentType.UNKNOWN);
        }
    }

    public enum MessageContentType {
        PLAIN(Plain.class),
        MEDIA(Media.class),
        COSMETIC_GIFT(CosmeticGift.class),
        SKIN(Skin.class),
        UNKNOWN(Unknown.class);

        private final Class<? extends MessageContent> typeClass;

        MessageContentType(Class<? extends MessageContent> typeClass) {
            this.typeClass = typeClass;
        }

        public Class<? extends MessageContent> getTypeClass() {
            return this.typeClass;
        }
    }

    public static class Serializer implements JsonSerializer<MessageContent>, JsonDeserializer<MessageContent> {
        private static final Logger LOGGER = LoggerFactory.getLogger(Serializer.class);

        @Override
        public MessageContent deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) {
            try {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                String messageContentTypeString = jsonObject.get("type").getAsString();
                MessageContentType messageContentType;
                try {
                    messageContentType = MessageContentType.valueOf(messageContentTypeString);
                } catch (final IllegalArgumentException e) {
                    messageContentType = MessageContentType.UNKNOWN;
                }
                return context.deserialize(jsonElement, messageContentType.getTypeClass());
            } catch (Exception e) {
                LOGGER.error("Error parsing message content", e);
                return new Unknown();
            }
        }

        @Override
        public JsonElement serialize(MessageContent messageContent, Type type, JsonSerializationContext context) {
            return context.serialize(messageContent);
        }
    }

}