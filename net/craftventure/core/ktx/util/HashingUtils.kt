package net.craftventure.core.ktx.util

import java.security.MessageDigest


object HashingUtils {
    // String = SHA-1, SHA-512 etc..
    fun hash(data: String, algorithm: String): ByteArray? {
        try {
            val digest: MessageDigest = MessageDigest.getInstance(algorithm)
            return digest.digest(data.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun printableHexString(data: ByteArray): String {
        // Create Hex String
        val hexString: StringBuilder = StringBuilder()
        for (aMessageDigest: Byte in data) {
            var h: String = Integer.toHexString(0xFF and aMessageDigest.toInt())
            while (h.length < 2)
                h = "0$h"
            hexString.append(h)
        }
        return hexString.toString()
    }
}

fun String.hash(algorithm: String) = HashingUtils.hash(this, algorithm)
fun ByteArray.printableHexString() = HashingUtils.printableHexString(this)