package io.github.marlonpya.dukpt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class KsnTest {

    @Test
    @DisplayName("El KSN se lee y se vuelve a escribir igual")
    fun roundTrip() {
        assertEquals("FFFF9876543210E00001", Ksn.parse("FFFF9876543210E00001").toHex())
    }

    @Test
    @DisplayName("El contador son los 21 bits de la derecha")
    fun counterIsTheLast21Bits() {
        assertEquals(0, Ksn.parse("FFFF9876543210E00000").counter)
        assertEquals(1, Ksn.parse("FFFF9876543210E00001").counter)
        assertEquals(0x1FF, Ksn.parse("FFFF9876543210E001FF").counter)
        assertEquals(0x1FF00, Ksn.parse("FFFF9876543210E1FF00").counter)
    }

    @Test
    @DisplayName("Cambiar el contador no toca el identificador del dispositivo")
    fun withCounterKeepsTheDeviceId() {
        val ksn = Ksn.parse("FFFF9876543210E00000").withCounter(0x1234)
        assertEquals("FFFF9876543210E01234", ksn.toHex())
        assertEquals(0x1234, ksn.counter)
    }

    @Test
    @DisplayName("El contador avanza saltando los valores con más de diez bits en uno")
    fun counterSkipsValuesWithTooManyOneBits() {
        val base = Ksn.parse("FFFF9876543210E00000")
        assertEquals(1, base.withCounter(0).next().counter)
        assertEquals(0x200, base.withCounter(0x1FF).next().counter)
        assertEquals(0x400, base.withCounter(0x3FF).next().counter)
        assertEquals(0x800, base.withCounter(0x7FE).next().counter)
    }

    @Test
    @DisplayName("Todo contador emitido respeta el límite de bits en uno")
    fun everyCounterStaysWithinTheOneBitLimit() {
        var ksn = Ksn.parse("FFFF9876543210E00000")
        var previous = ksn.counter
        repeat(5_000) {
            ksn = ksn.next()
            assertTrue(ksn.counter > previous, "El contador debe crecer")
            assertTrue(
                Integer.bitCount(ksn.counter) <= Ksn.MAX_ONE_BITS,
                "El contador ${ksn.counter} tiene más de ${Ksn.MAX_ONE_BITS} bits en uno",
            )
            previous = ksn.counter
        }
    }

    @Test
    @DisplayName("Al agotarse el contador el terminal debe pedir una nueva IPEK")
    fun exhaustedCounterFails() {
        val ksn = Ksn.parse("FFFF9876543210E00000").withCounter(0x1FF800)
        assertThrows(KsnExhaustedException::class.java) { ksn.next() }
    }

    @Test
    @DisplayName("Un KSN de largo equivocado se rechaza")
    fun malformedKsnIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { Ksn.parse("FFFF9876543210E000") }
    }
}
