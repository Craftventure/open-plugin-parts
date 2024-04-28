package net.craftventure.core.script.fixture.property

import io.reactivex.Flowable
import net.craftventure.core.script.timeline.KeyFrameEasing
import java.util.*

interface ObjectProperty<T> {
    val name: String
    val clazz: Class<T>
    val inEasingOverride: Optional<KeyFrameEasing>
    val outEasingOverride: Optional<KeyFrameEasing>

//    fun asString(): String

//    fun fromString(input: String):T

    fun asFlowable(): Flowable<T>
}