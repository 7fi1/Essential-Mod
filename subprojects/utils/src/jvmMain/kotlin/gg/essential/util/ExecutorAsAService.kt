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

import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class ExecutorAsAService(val executor: Executor) : AbstractExecutorService() {
    override fun execute(command: Runnable) = executor.execute(command)
    override fun isShutdown(): Boolean = false
    override fun isTerminated(): Boolean = false
    override fun shutdown() = throw UnsupportedOperationException()
    override fun shutdownNow(): List<Runnable?> = throw UnsupportedOperationException()
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = throw UnsupportedOperationException()
}
