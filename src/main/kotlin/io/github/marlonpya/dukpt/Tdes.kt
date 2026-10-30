package io.github.marlonpya.dukpt

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * DES y TDES en modo ECB sin relleno, que es lo que usa la derivación DUKPT.
 *
 * ECB sin relleno aquí no es un descuido: se cifran bloques de 8 bytes de largo
 * fijo (registros de llave y PIN blocks), no mensajes. Para datos de longitud
 * variable corresponde otro modo.
 */
internal object Tdes {

    private const val DES_BLOCK = 8

    /** TDES de dos llaves (K1, K2, K1) sobre un bloque de 8 bytes. */
    fun encrypt(key16: ByteArray, block: ByteArray): ByteArray =
        run(Cipher.ENCRYPT_MODE, "DESede", expand(key16), block)

    fun decrypt(key16: ByteArray, block: ByteArray): ByteArray =
        run(Cipher.DECRYPT_MODE, "DESede", expand(key16), block)

    /** DES simple, usado dentro del proceso de generación de llave no reversible. */
    fun desEncrypt(key8: ByteArray, block: ByteArray): ByteArray =
        run(Cipher.ENCRYPT_MODE, "DES", key8, block)

    private fun expand(key16: ByteArray): ByteArray {
        require(key16.size == 16) { "La llave TDES debe tener 16 bytes, tiene ${key16.size}" }
        val k24 = ByteArray(24)
        key16.copyInto(k24, 0)
        key16.copyInto(k24, 16, 0, 8)
        return k24
    }

    private fun run(mode: Int, algorithm: String, key: ByteArray, block: ByteArray): ByteArray {
        require(block.size % DES_BLOCK == 0) {
            "El bloque debe ser múltiplo de $DES_BLOCK bytes, tiene ${block.size}"
        }
        val cipher = Cipher.getInstance("$algorithm/ECB/NoPadding")
        cipher.init(mode, SecretKeySpec(key, algorithm))
        return cipher.doFinal(block)
    }
}
