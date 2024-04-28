package net.craftventure.core.ktx.concurrency

import kotlinx.coroutines.asCoroutineDispatcher

object CoreDispatchers {
    val Executor = CvExecutors.executor.asCoroutineDispatcher()
}