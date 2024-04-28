package net.craftventure.core.animation

class IndexedIterationList<T> : ArrayList<T>() {
    var index = 0

    fun increaseIndex(loop: Boolean): Boolean {
        if (index + 1 < size) {
            index++
            return true
        } else if (index + 1 >= size && loop) {
            index++
            index = index % size
            return true
        }
        return false
    }

    val current: T?
        get() {
            if (size > index) return get(index)
            return if (size > 0) get(size - 1) else null
        }

    fun getNext(loop: Boolean): T? {
        if (size > index + 1) return get(index + 1) else if (loop && !isEmpty()) {
            return get(0)
        }
        return current
    }
}