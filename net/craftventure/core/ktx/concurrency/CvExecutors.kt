package net.craftventure.core.ktx.concurrency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import net.craftventure.core.ktx.util.Logger.capture
import java.util.concurrent.ScheduledThreadPoolExecutor

object CvExecutors {
    val executor by lazy { Dispatchers.IO.asExecutor() }
    val scheduledExecutor by lazy {
        ScheduledThreadPoolExecutor(
            10, ThreadFactoryBuilder()
                .setUncaughtExceptionHandler { t: Thread?, e: Throwable? ->
                    capture(
                        e!!
                    )
                }
                .setNameFormat("cv-scheduled-%d").build()
        )
    }
}