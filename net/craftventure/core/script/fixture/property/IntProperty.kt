package net.craftventure.core.script.fixture.property

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import net.craftventure.core.script.timeline.KeyFrameEasing
import java.util.*

class IntProperty(
    name: String,
    initialValue: Int,
    min: Int? = null,
    max: Int? = null,
    override val inEasingOverride: Optional<KeyFrameEasing> = Optional.empty(),
    override val outEasingOverride: Optional<KeyFrameEasing> = Optional.empty()
) : NumberProperty<Number>(
    Number::class.java,
    name,
    initialValue,
    default = initialValue,
    min = min,
    max = max
) {
    override fun convert(input: Number): Number = Math.round(input.toDouble()).toInt()

    override fun set(newValue: Number) {
        value.onNext(Math.round(newValue.toDouble()).toInt())
    }

    override fun get() = value.value!!.toInt()

//    override fun asString(): String {
//        return value.value.toInt().toString()
//    }
//
//    override fun fromString(input: String): Number {
//        return input.toInt()
//    }

    override fun asFlowable(): Flowable<Number> = value.toFlowable(BackpressureStrategy.LATEST)
}