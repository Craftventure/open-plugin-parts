package net.craftventure.core.script.timeline

import net.craftventure.core.ktx.extension.orIfNan
import net.craftventure.core.script.fixture.property.NumberProperty

class Timeline<T : NumberProperty<*>>(val property: T) {
    var keyframes = emptyList<KeyFrame>()
        private set

    init {
        addKeyframe(KeyFrame(0, property.default.toDouble()))
//        keyframes.autoSort { it.at }
//        keyframes.onChange {
//            keyframes.sortBy { it.at }
//        }
    }

    fun removeKeyFrame(keyFrame: KeyFrame): Boolean {
        val newFrames = keyframes.toMutableList()
        val result = newFrames.remove(keyFrame)
        keyframes = newFrames.toList()
        return result
    }

    fun addKeyframe(keyFrame: KeyFrame) {
        keyframes = keyframes.toMutableList().also {
            it.add(keyFrame).let {
                if (it) {
                    clampAll()
                }
            }
        }.toList()
    }

    fun clampAll() {
        for (keyFrame in keyframes) {
            keyFrame.keyValue = property.clamp(keyFrame.keyValue)
        }
    }

    fun resortIfUnsorted() {
        var lastAt = 0L
        var resort = false
        for (keyframe in keyframes) {
            if (keyframe.at < lastAt) {
                resort = true
                break
            }
            lastAt = keyframe.at
        }
        if (resort) {
            keyframes = keyframes.sortedBy { it.at }
        }
    }

    fun getFrameTimeBetween(startSeconds: Double, endSeconds: Double): Long? {
        val startMillis = startSeconds * 1000
        val endMillis = endSeconds * 1000
        for (frame in keyframes) {
            if (frame.at.toDouble() in startMillis..endMillis) {
                return frame.at
            }
        }
        return null
    }

    fun valueAt(seconds: Double): Double {
        val millis = seconds * 1000
        if (keyframes.size <= 1) {
            keyframes.firstOrNull()?.let {
                if (it.at <= millis)
                    return safe(it.keyValue.toDouble()).toDouble().orIfNan(0.0)
            }
        } else {
            for ((index, keyframe) in keyframes.withIndex()) {
                if (index == 0) continue
                if (keyframe.at < millis) continue

                val previousKeyframe = keyframes[index - 1]
                if (previousKeyframe.at <= millis && keyframe.at >= millis) {
                    val percentage = (millis - previousKeyframe.at) / (keyframe.at - previousKeyframe.at)
                    if (previousKeyframe.outEasing == KeyFrameEasing.NONE && keyframe.inEasing == KeyFrameEasing.NONE) {
                        return safe(previousKeyframe.keyValue.toDouble() + ((keyframe.keyValue.toDouble() - previousKeyframe.keyValue.toDouble()) * percentage)).toDouble()
                            .orIfNan(0.0)
                    } else {
                        val previousOutEasing = property.outEasingOverride.orElse(previousKeyframe.outEasing)
                        val nextInEasing = property.inEasingOverride.orElse(keyframe.inEasing)

                        if (nextInEasing == KeyFrameEasing.PREVIOUS)
                            return safe(previousKeyframe.keyValue).toDouble().orIfNan(0.0)

                        val tPrevious = previousOutEasing.easing(
                            (millis - previousKeyframe.at),
                            0.0,
                            1.0,
                            (keyframe.at - previousKeyframe.at).toDouble()
                        )
                        val tNext = nextInEasing.easing(
                            (millis - previousKeyframe.at),
                            0.0,
                            1.0,
                            (keyframe.at - previousKeyframe.at).toDouble()
                        )
                        val tPreviousValue =
                            (previousKeyframe.keyValue.toDouble() * (1 - tPrevious) + keyframe.keyValue.toDouble() * tPrevious)
                        val tNextValue =
                            (previousKeyframe.keyValue.toDouble() * (1 - tNext) + keyframe.keyValue.toDouble() * tNext)

                        if (previousOutEasing == KeyFrameEasing.NONE)
                            return safe(tNextValue).toDouble().orIfNan(0.0)
                        else if (nextInEasing == KeyFrameEasing.NONE)
                            return safe(tPreviousValue).toDouble().orIfNan(0.0)

                        return safe(tPreviousValue * (1 - percentage) + tNextValue * percentage).toDouble()//tNextValue//((tPreviousValue * percentage) + (tNext * percentage))
                    }
//                    return previousKeyframe.keyValue.toDouble() + ((keyframe.keyValue.toDouble() - previousKeyframe.keyValue.toDouble()) * percentage)
                }
            }
            val last = keyframes.last()
            if (last.at < millis)
                return safe(last.keyValue.toDouble()).toDouble().orIfNan(0.0)
        }
        return safe(property.default).toDouble().orIfNan(0.0)
    }

    private fun safe(input: Number) = property.convert(property.clamp(input))
}