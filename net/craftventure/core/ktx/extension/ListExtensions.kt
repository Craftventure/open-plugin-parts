package net.craftventure.core.ktx.extension

inline fun <T> List<T>.forEachAllocationless(crossinline run: (T) -> Unit) {
    for (i in 0 until size) {
        run.invoke(get(i))
    }
}