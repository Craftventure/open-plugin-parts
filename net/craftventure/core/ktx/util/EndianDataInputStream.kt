package net.craftventure.core.ktx.util

import java.io.DataInput
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Taken from https://gist.github.com/MichaelBeeu/6545110

/**
 * Simple class to add endian support to DataInputStream.
 * User: michael
 * Date: 9/12/13
 * Time: 4:39 PM
 */
class EndianDataInputStream(
    stream: InputStream?,
    private val order: ByteOrder = ByteOrder.BIG_ENDIAN
) : InputStream(),
    DataInput {
    var dataIn: DataInputStream
    private val buffer = ByteBuffer.allocate(8)

    init {
        dataIn = DataInputStream(stream)
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        return dataIn.read(b)
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return dataIn.read(b, off, len)
    }

    @Deprecated("")
    @Throws(IOException::class)
    override fun readLine(): String {
        return dataIn.readLine()
    }

    @Throws(IOException::class)
    override fun readBoolean(): Boolean {
        return dataIn.readBoolean()
    }

    @Throws(IOException::class)
    override fun readByte(): Byte {
        return dataIn.readByte()
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return readByte().toInt()
    }

    override fun markSupported(): Boolean {
        return dataIn.markSupported()
    }

    override fun mark(readlimit: Int) {
        dataIn.mark(readlimit)
    }

    @Throws(IOException::class)
    override fun reset() {
        dataIn.reset()
    }

    @Throws(IOException::class)
    override fun readChar(): Char {
        return dataIn.readChar()
    }

    @Throws(IOException::class)
    override fun readFully(b: ByteArray) {
        dataIn.readFully(b)
    }

    @Throws(IOException::class)
    override fun readFully(b: ByteArray, off: Int, len: Int) {
        dataIn.readFully(b, off, len)
    }

    @Throws(IOException::class)
    override fun readUTF(): String {
        return dataIn.readUTF()
    }

    @Throws(IOException::class)
    override fun skipBytes(n: Int): Int {
        return dataIn.skipBytes(n)
    }

    @Throws(IOException::class)
    override fun readDouble(): Double {
        val tmp = readLong()
        return java.lang.Double.longBitsToDouble(tmp)
    }

    @Throws(IOException::class)
    override fun readFloat(): Float {
        val tmp = readInt()
        return java.lang.Float.intBitsToFloat(tmp)
    }

    @Throws(IOException::class)
    override fun readInt(): Int {
        buffer.clear()
        buffer.order(ByteOrder.BIG_ENDIAN)
            .putInt(dataIn.readInt())
            .flip()
        return buffer.order(order).int
    }

    @Throws(IOException::class)
    override fun readLong(): Long {
        buffer.clear()
        buffer.order(ByteOrder.BIG_ENDIAN)
            .putLong(dataIn.readLong())
            .flip()
        return buffer.order(order).long
    }

    @Throws(IOException::class)
    override fun readShort(): Short {
        buffer.clear()
        buffer.order(ByteOrder.BIG_ENDIAN)
            .putShort(dataIn.readShort())
            .flip()
        return buffer.order(order).short
    }

    @Throws(IOException::class)
    override fun readUnsignedByte(): Int {
        return dataIn.readByte().toInt()
    }

    @Throws(IOException::class)
    override fun readUnsignedShort(): Int {
        return readShort().toInt()
    }
}