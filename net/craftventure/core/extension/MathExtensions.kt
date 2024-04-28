package net.craftventure.core.extension

import net.craftventure.core.utils.InterpolationUtils

inline fun Double.progressTo(newValue: Double, progress: Double) =
    InterpolationUtils.linearInterpolate(this, newValue, progress)

inline fun Float.progressTo(newValue: Float, progress: Double) =
    InterpolationUtils.linearInterpolate(this.toDouble(), newValue.toDouble(), progress).toFloat()