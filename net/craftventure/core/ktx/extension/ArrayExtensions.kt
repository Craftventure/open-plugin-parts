package net.craftventure.core.ktx.extension

import java.util.concurrent.ThreadLocalRandom

@Deprecated(message = "Use Kotlin built in random")
fun <T> Array<T>.random(): T? {
    if (this.isEmpty()) return null
    return this[ThreadLocalRandom.current().nextInt(size)]
}

@Deprecated(message = "Use Kotlin built in random")
fun <T> List<T>.random(): T? {
    if (this.isEmpty()) return null
    return this[ThreadLocalRandom.current().nextInt(size)]
}

fun <T> List<T>.random(offset: Int, max: Int): T? {
    if (this.isEmpty()) return null
    return this[(offset - 1) + ThreadLocalRandom.current().nextInt(Math.min(size, offset + max))]
}