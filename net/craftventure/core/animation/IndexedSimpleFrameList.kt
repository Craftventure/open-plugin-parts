package net.craftventure.core.animation

import net.craftventure.core.animation.keyframed.SimpleKeyFrame
import java.util.*

class IndexedSimpleFrameList<T : SimpleKeyFrame?> : ArrayList<T>() {
    var index = 0
    fun updateIndexForTime(showTime: Double) {
        for (i in index until size) {
            val frame: SimpleKeyFrame? = get(i)
            if (frame!!.time > showTime) {
                return
            }
            index = i
        }
    }

    fun increaseIndex(): Int {
        index++
        return index
    }

    val current: T?
        get() {
            if (size > index) return get(index)
            return if (size > 0) get(size - 1) else null
        }

    val next: T?
        get() = if (size > index + 1) get(index + 1) else current

    fun sort() {
        Collections.sort(this, Comparator { o1, o2 ->
            if (o1!!.time > o2!!.time) return@Comparator 1
            if (o1.time < o2.time) -1 else 0
        })
    }
}