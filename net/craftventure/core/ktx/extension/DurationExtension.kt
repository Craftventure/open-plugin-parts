package net.craftventure.core.ktx.extension

import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration

fun Duration.asTicks() = (inWholeMilliseconds / 50.0).roundToLong()
fun Duration.asTicksInt() = (inWholeMilliseconds / 50.0).roundToInt()