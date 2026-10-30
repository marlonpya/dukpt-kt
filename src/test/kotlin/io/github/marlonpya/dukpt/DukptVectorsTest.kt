package io.github.marlonpya.dukpt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Vectores de prueba de ANSI X9.24-1 Annex A.4, con la BDK y el KSN inicial
 * clásicos del anexo. Las llaves derivadas se contrastaron además contra una
 * implementación independiente del estándar.
 */
class DukptVectorsTest {

    private val bdk = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
    private val initialKsn = Ksn.parse("FFFF9876543210E00000")
    private val pin = "1234"
    private val pan = "4012345678909"

    private data class Vector(
        val ksn: String,
        val transactionKey: String,
        val pinKey: String,
        val macRequestKey: String,
        val encryptedPinBlock: String,
    )

    private val vectors = listOf(
        Vector("FFFF9876543210E00000", "6AC292FAA1315B4D858AB3A3D7D5933A", "6AC292FAA1315BB2858AB3A3D7D593C5", "6AC292FAA131A44D858AB3A3D7D56C3A", ""),
        Vector("FFFF9876543210E00001", "042666B49184CFA368DE9628D0397BC9", "042666B49184CF5C68DE9628D0397B36", "042666B4918430A368DE9628D03984C9", "1B9C1845EB993A7A"),
        Vector("FFFF9876543210E00002", "C46551CEF9FD24B0AA9AD834130D3BC7", "C46551CEF9FD244FAA9AD834130D3B38", "C46551CEF9FDDBB0AA9AD834130DC4C7", "10A01C8D02C69107"),
        Vector("FFFF9876543210E00003", "0DF3D9422ACA56E547676D07AD6BADFA", "0DF3D9422ACA561A47676D07AD6BAD05", "0DF3D9422ACAA9E547676D07AD6B52FA", "18DC07B94797B466"),
        Vector("FFFF9876543210E00004", "279C0F6AEED0BE652B2C733E1383AE91", "279C0F6AEED0BE9A2B2C733E1383AE6E", "279C0F6AEED041652B2C733E13835191", "0BC79509D5645DF7"),
        Vector("FFFF9876543210E00005", "5F8DC6D2C845C125508DDC048093B83F", "5F8DC6D2C845C1DA508DDC048093B8C0", "5F8DC6D2C8453E25508DDC048093473F", "5BC0AF22AD87B327"),
        Vector("FFFF9876543210E00008", "27F66D5244FF62E1AA6F6120EDEB4280", "27F66D5244FF621EAA6F6120EDEB427F", "27F66D5244FF9DE1AA6F6120EDEBBD80", "50E55547A5027551"),
        Vector("FFFF9876543210E0000A", "6CF2500A22507C7CC776CEADC1E33014", "6CF2500A22507C83C776CEADC1E330EB", "6CF2500A2250837CC776CEADC1E3CF14", "EDABBA23221833FE"),
        Vector("FFFF9876543210E00010", "59598DCBD9BD94C094165CE453585F57", "59598DCBD9BD943F94165CE453585FA8", "59598DCBD9BD6BC094165CE45358A057", "D5D9638559EF53D6"),
        Vector("FFFF9876543210E001FF", "01A97DB82D68BA32F6CA68887833212B", "01A97DB82D68BACDF6CA6888783321D4", "01A97DB82D684532F6CA68887833DE2B", "27196155375DBC4A"),
        Vector("FFFF9876543210E00800", "7E4AB005422BCADC5F65363964EF6504", "7E4AB005422BCA235F65363964EF65FB", "7E4AB005422B35DC5F65363964EF9A04", "7D690D85FFA4878E"),
        Vector("FFFF9876543210E1FF00", "7F69B9469600A0D5B51BE0FE99EAA33B", "7F69B9469600A02AB51BE0FE99EAA3C4", "7F69B94696005FD5B51BE0FE99EA5C3B", "833DD3BE691E1E43"),
    )

    @Test
    @DisplayName("La IPEK del anexo A.4 se deriva de la BDK y el KSN inicial")
    fun ipekMatchesTheAnnexVector() {
        assertEquals(
            "6AC292FAA1315B4D858AB3A3D7D5933A",
            Dukpt.deriveIpek(bdk, initialKsn).toHex(),
        )
    }

    @Test
    @DisplayName("Con el contador en cero, la llave de transacción es la IPEK")
    fun transactionKeyAtCounterZeroIsTheIpek() {
        val ipek = Dukpt.deriveIpek(bdk, initialKsn)
        assertEquals(ipek.toHex(), Dukpt.deriveTransactionKey(ipek, initialKsn).toHex())
    }

    @Test
    @DisplayName("Cada KSN produce su propia llave de transacción")
    fun transactionKeysMatchTheVectors() {
        for (v in vectors) {
            val ksn = Ksn.parse(v.ksn)
            assertEquals(v.transactionKey, Dukpt.transactionKey(bdk, ksn).toHex(), "KSN ${v.ksn}")
        }
    }

    @Test
    @DisplayName("Las variantes de PIN y de MAC salen de la llave de transacción")
    fun variantsMatchTheVectors() {
        for (v in vectors) {
            val key = Dukpt.transactionKey(bdk, Ksn.parse(v.ksn))
            assertEquals(v.pinKey, Dukpt.pinKey(key).toHex(), "PIN, KSN ${v.ksn}")
            assertEquals(v.macRequestKey, Dukpt.macRequestKey(key).toHex(), "MAC, KSN ${v.ksn}")
        }
    }

    @Test
    @DisplayName("Ninguna llave se repite entre transacciones")
    fun everyTransactionGetsADistinctKey() {
        val keys = vectors.map { Dukpt.transactionKey(bdk, Ksn.parse(it.ksn)).toHex() }
        assertEquals(keys.size, keys.toSet().size, "Hay llaves repetidas")
    }

    @Test
    @DisplayName("El PIN block en claro combina PIN y PAN")
    fun clearPinBlockMatchesTheVector() {
        assertEquals("041274EDCBA9876F", PinBlock.encode(pin, pan).toHex())
    }

    @Test
    @DisplayName("El PIN block cifrado coincide con los vectores")
    fun encryptedPinBlocksMatchTheVectors() {
        for (v in vectors.filter { it.encryptedPinBlock.isNotEmpty() }) {
            val key = Dukpt.transactionKey(bdk, Ksn.parse(v.ksn))
            assertEquals(v.encryptedPinBlock, PinBlock.encrypt(key, pin, pan).toHex(), "KSN ${v.ksn}")
        }
    }

    @Test
    @DisplayName("El host recupera el PIN desde el KSN y la BDK")
    fun hostRecoversThePin() {
        val ksn = Ksn.parse("FFFF9876543210E00001")
        val terminalKey = Dukpt.transactionKey(bdk, ksn)
        val encrypted = PinBlock.encrypt(terminalKey, pin, pan)

        // El host solo recibe el KSN y el bloque cifrado.
        val hostKey = Dukpt.transactionKey(bdk, ksn)
        assertEquals(pin, PinBlock.decrypt(hostKey, encrypted, pan))
    }

    @Test
    @DisplayName("Con otro PAN no se recupera el PIN original")
    fun aDifferentPanDoesNotYieldTheOriginalPin() {
        val ksn = Ksn.parse("FFFF9876543210E00001")
        val key = Dukpt.transactionKey(bdk, ksn)
        val encrypted = PinBlock.encrypt(key, pin, pan)

        // El formato 0 no autentica: con el PAN equivocado unas veces falla el
        // descifrado y otras devuelve dígitos distintos, sin avisar. La
        // integridad del mensaje la da el MAC, no el PIN block.
        val recovered = runCatching { PinBlock.decrypt(key, encrypted, "4999945678909") }.getOrNull()
        assertTrue(recovered != pin, "Con otro PAN no debería salir el PIN original")
    }

    @Test
    @DisplayName("El dígito verificador del PAN no participa en el PIN block")
    fun theCheckDigitIsNotPartOfThePanField() {
        assertEquals(
            PinBlock.encode(pin, "4012345678909").toHex(),
            PinBlock.encode(pin, "4012345678901").toHex(),
        )
    }

    @Test
    @DisplayName("Una llave de largo equivocado se rechaza")
    fun malformedKeysAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Dukpt.deriveIpek("0123456789ABCDEF".hexToBytes(), initialKsn)
        }
    }
}
