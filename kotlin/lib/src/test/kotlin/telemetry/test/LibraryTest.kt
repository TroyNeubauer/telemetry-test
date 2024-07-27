/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package telemetry.test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Timeout
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.util.Arrays
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue

class LibraryTest {
    @Timeout(15, unit = TimeUnit.SECONDS)
    @Test fun someLibraryMethodReturnsTrue() {
        val socket = Socket()
        socket.connect(InetSocketAddress("127.0.0.1", 6969))
        val reader = ProtocolReader(BufReader(socket.getInputStream()))
        var i = 0
        var byte = 1
        while (true) {
            val payload = reader.nextPayload()
            if (payload != null) {
                i++
                println("Accepted pakcet")
                /*for (b in payload.iterator()) {
                    assertEquals(byte.toUByte().toByte(), b, "Payload bytes differ")
                    byte += 1
                }*/
            } else {
                println("Failed to read packet")
            }
            if (i >= 1000) {
                break;
            }
            //println("Got payload: ${Arrays.toString(payload)}")
        }
    }
}
