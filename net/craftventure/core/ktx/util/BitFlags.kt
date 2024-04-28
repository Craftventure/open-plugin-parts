@file:Suppress("NOTHING_TO_INLINE")

package net.craftventure


import kotlin.experimental.and // Used for Byte
import kotlin.experimental.inv // Used for Byte
import kotlin.experimental.or // Used for Byte

inline infix fun Int.hasFlag(flag: Int) = flag and this == flag
inline infix fun Int.withFlag(flag: Int) = this or flag
inline infix fun Int.minusFlag(flag: Int) = this and flag.inv()

inline infix fun Byte.hasFlag(flag: Byte) = flag and this == flag
inline infix fun Byte.withFlag(flag: Byte) = this or flag
inline infix fun Byte.minusFlag(flag: Byte) = this and flag.inv()