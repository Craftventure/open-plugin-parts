package net.craftventure.core.script.fixture.property

import io.reactivex.subjects.BehaviorSubject

abstract class NumberProperty<T : Number>(
    override val clazz: Class<T>,
    override val name: String,
    initialValue: T,
    val default: T,
    val min: T? = null,
    val max: T? = null
) : ObjectProperty<T> {
    protected var value = BehaviorSubject.createDefault<T>(initialValue)

//    fun asString(): String = value.toString()

    abstract fun set(newValue: Number)

    abstract fun get(): Number

    abstract fun convert(input: Number): Number

    fun clamp(value: Number): Number {
        var clamped = value.toDouble()
        min?.let { clamped = Math.max(min.toDouble(), clamped) }
        max?.let { clamped = Math.min(max.toDouble(), clamped) }
        return clamped
    }
}