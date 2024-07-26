@file:OptIn(ExperimentalUnsignedTypes::class)

package telemetry.test

import java.io.IOException
import java.io.InputStream
import java.util.Arrays

val MAGIC = ubyteArrayOf('A'.code.toUByte(), 'B'.code.toUByte(), 'C'.code.toUByte())

fun calculateCrc(buf: ByteArray, offset: Int, len: Int): UShort {
    var crc = 0x6969
    var i = 0
    while(i < len) {
        //print("${buf[offset + i].toUByte()} ")
        crc += (buf[offset + i]).toUByte().toInt()
        i++
    }
    //println(" CRC complete: ${crc.toUShort()}")
    return crc.toUShort()
}

class ProtocolReader(private val reader: BufReader) {
    // Advances to the next magic, validates the crc and returns the payload.
    // Throws if EOF is reached
    fun nextPayload(): ByteArray? {
        this.reader.recordMark()
        if (this.reader.readByte() == MAGIC[0]
            && this.reader.readByte() == MAGIC[1]
            && this.reader.readByte() == MAGIC[2])
        {
            //println(">Read valid magic")
            val payloadLen = this.reader.readU16LE().toInt()
            //println(">Got len $payloadLen")
            val payload = this.reader.readBytes(payloadLen)
            val crc = this.reader.readU16LE()
            val mark = this.reader.getMark()

            val packetLen = this.reader.filled - mark
            //println("filled: ${this.reader.filled}, mark: $mark")
            //println("packetLen: $packetLen, payloadLen: $payloadLen")
            assert(packetLen == payloadLen + 3 + 2 + 2)
            // dont include crc when calculating itself
            val expectedCrc = calculateCrc(reader.buf, mark, packetLen - 2)
            if (crc != expectedCrc) {
                println(">Throwing out invalid crc. Expected $expectedCrc, got $crc")
                return null
            }
            return payload
        } else {
            println(">Got invalid magic")
        }
        return null
    }
}

// offset is the index of the next byte to be read, len is the bytes remaining in the unread portion
class BufReader(private val inner: InputStream) {
    val buf: ByteArray = ByteArray(32) { 0 }
    var filled: Int = 0
    var initialized: Int = 0
    // Mark is used to record the first index of interest when shifting bytes backward
    // Useful for keeping an entire packet continuous, even with dynamic parts requiring multiple reads
    private var mark: Int = -1

    // Reads a single byte, or throws IoException if EOF is encountered
    // Throws if EOF is reached
    fun readByte(): UByte {
        if (this.isEmpty()) {
            this.fillAtLeast(1)
        }
        val b = this.buf[this.filled].toUByte()
        this.consume(1)
        return b
    }

    // Throws if EOF is reached
    fun readU16LE(): UShort {
        val low = this.readByte()
        val high = this.readByte()
        return high.toInt().shl(8).or(low.toInt()).toUShort()
    }

    // Throws if EOF is reached
    fun readBytes(len: Int): ByteArray {
        this.fillAtLeast(len)
        val dst = Arrays.copyOfRange(this.buf, this.filled, len + this.filled)
        this.consume(len)
        return dst
    }

    // Advances the internal pointer by `count` bytes. Must be called after `fillAtLeast` with count being the same or larger
    fun consume(count: Int) {
        if (this.initialized < count || this.filled + count > this.buf.size) {
            throw RuntimeException("Buffer overflow")
        }
        this.filled += count
    }

    // Calls read until at least `count` bytes are buffered
    // Throws if EOF is reached
    fun fillAtLeast(count: Int) {
        //println();
        //println("fillAtLeast($count)");
        while (true) {
            if (this.bufferedCount() >= count) {
                return
            }
            if (count > this.buf.size) {
                throw RuntimeException("Requested read larger than buffer size: ${this.buf.size}, request $count")
            }

            val required = count - this.bufferedCount()
            if (required > this.unfilledCount()) {
                if (mark == -1) {
                    val buffered = this.bufferedCount()
                    println("Not enough space in buffer ${this.unfilledCount()}/${buf.size} for fill of $count. Shifting $buffered bytes back to start of buffer")
                    // need to shift bytes back to read again
                    System.arraycopy(this.buf, this.filled, this.buf, 0, buffered)
                    this.initialized -= this.filled
                    this.filled -= this.filled
                } else {
                    val buffered = this.initialized - this.mark
                    println("Not enough space in buffer ${this.unfilledCount()}/${buf.size} for fill of $count. Shifting $buffered bytes back to start of buffer from mark ${this.mark}")
                    println("Copying index $mark to index 0 in buf")
                    // need to shift bytes back to read again
                    System.arraycopy(this.buf, this.mark, this.buf, 0, buffered)
                    this.filled -= this.mark
                    this.initialized -= this.mark
                    this.mark -= this.mark
                    //println("First bytes after copy: ${Arrays.toString(Arrays.copyOfRange(this.buf, this.mark, this.initialized))}")
                }
            }
            val read = this.fillBuf()
            if (read == -1) {
                throw IOException()
            }
            if (read >= required) {
                return
            }
        }
    }

    fun fillBuf(): Int {
        println()
        val read = this.inner.read(buf, this.initialized, this.unfilledCount())
        //println("First bytes after read: ${Arrays.toString(Arrays.copyOfRange(this.buf, this.filled, this.filled + this.bufferedCount()))}")

        println("Read $read bytes")
        this.initialized += read
        println("BUF state: filled: ${this.filled}, initialized: ${this.initialized}, buffered: ${this.bufferedCount()}")
        return read
    }

    fun recordMark() {
        this.mark = this.filled
    }

    fun resetMark() {
        this.mark = this.filled
    }

    fun getMark(): Int {
        if (this.mark == -1) {
            throw RuntimeException("mark not set")
        }
        return this.mark
    }

    // returns the number of bytes available to read new bytes into
    private fun unfilledCount(): Int {
        return this.buf.size - this.initialized
    }

    private fun bufferedCount(): Int {
        return this.initialized - this.filled
    }

    // Returns true if there are no more buffered bytes left to read
    private fun isEmpty(): Boolean {
        return this.bufferedCount() == 0
    }
}
