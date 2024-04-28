package net.craftventure.core.script.fixture.property

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import net.craftventure.core.script.timeline.KeyFrameEasing
import java.util.*

class DoubleProperty(
    name: String,
    initialValue: Double,
    min: Double? = null,
    max: Double? = null,
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
    override fun convert(input: Number): Number = input.toDouble()

    override fun set(newValue: Number) {
        value.onNext(Math.round(newValue.toDouble()).toInt())
    }

    override fun get() = value.value!!.toDouble()

//    override fun asString(): String {
//        return value.value.toDouble().toString()
//    }
//
//    override fun fromString(input: String): Number {
//        return input.toDouble()
//    }

    override fun asFlowable(): Flowable<Number> = value.toFlowable(BackpressureStrategy.LATEST)
}