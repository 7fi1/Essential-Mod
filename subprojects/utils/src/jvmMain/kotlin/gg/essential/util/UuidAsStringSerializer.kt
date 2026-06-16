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
package gg.essential.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UuidAsStringSerializer : KSerializer<UUID> {
    private val inner = String.serializer()
    override val descriptor = inner.descriptor

    override fun serialize(encoder: Encoder, value: UUID) =
        encoder.encodeSerializableValue(inner, value.toString())

    override fun deserialize(decoder: Decoder): UUID =
        UUID.fromString(decoder.decodeSerializableValue(inner))
}
