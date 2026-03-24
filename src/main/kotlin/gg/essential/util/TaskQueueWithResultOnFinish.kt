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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class TaskQueueWithResultOnFinish<T>(
    coroutineScope: CoroutineScope,
    private val onFinishCoroutineScope: CoroutineScope,
    private val onFinishOutput: (T) -> Unit
) {

    private val channel = Channel<Pair<Int, suspend () -> T>>(Channel.UNLIMITED)
    private val latestId = AtomicInteger(0)

    init {
        coroutineScope.launch {
            for ((id, task) in channel) {
                val output = task()

                onFinishCoroutineScope.launch {
                    if (latestId.get() == id) {
                        onFinishOutput(output)
                    }
                }
            }
        }
    }

    fun enqueue(task: suspend () -> T) = channel.trySend(latestId.incrementAndGet() to task)

}