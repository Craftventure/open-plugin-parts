package net.craftventure.core.script.fixture.property

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.script.timeline.KeyFrameEasing
import java.util.*

class BooleanProperty(
    name: String,
    initialValue: Boolean,
    override val inEasingOverride: Optional<KeyFrameEasing> = Optional.empty(),
    override val outEasingOverride: Optional<KeyFrameEasing> = Optional.empty()
) : NumberProperty<Number>(
    Number::class.java,
    name,
    if (initialValue) 1.0 else 0.0,
    default = 0.0,
    min = 0.0,
    max = 1.0
) {
    override fun convert(input: Number): Number = Math.round(input.toDouble().clamp(0.0, 1.0)).toInt()

    override fun set(newValue: Number) {
        value.onNext(Math.round(newValue.toDouble().clamp(0.0, 1.0)).toInt())
    }

    override fun get() = value.value!!.toDouble().clamp(0.0, 1.0).toInt()

//    override fun asString(): String {
//        return value.value.toDouble().toString()
//    }
//
//    override fun fromString(input: String): Number {
//        return input.toDouble()
//    }

    override fun asFlowable(): Flowable<Number> = value.toFlowable(BackpressureStrategy.LATEST)
}