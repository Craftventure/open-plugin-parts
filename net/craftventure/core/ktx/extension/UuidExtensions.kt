package net.craftventure.core.ktx.extension

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

fun String.toUuid() = UUID.fromString(this)

fun UUID.toBinary(): ByteArray {
    val uuidBytes = ByteArray(16)
    ByteBuffer.wrap(uuidBytes)
        .order(ByteOrder.BIG_ENDIAN)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
    return uuidBytes
}

fun ByteArray.toUuid(): UUID {
    require(size == 16)
    val buf = ByteBuffer.wrap(this)
        .order(ByteOrder.BIG_ENDIAN)
    return UUID(buf.long, buf.long)
}