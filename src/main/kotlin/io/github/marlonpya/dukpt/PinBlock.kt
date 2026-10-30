package io.github.marlonpya.dukpt

/**
 * PIN block ISO 9564-1 formato 0.
 *
 * El PIN se combina con el PAN antes de cifrarse, de modo que el mismo PIN en
 * dos tarjetas distintas produce bloques distintos. En un terminal real este
 * bloque se arma dentro del hardware seguro: el PIN en claro no pasa por la
 * aplicación en ningún momento.
 */
object PinBlock {

    private const val BLOCK_SIZE = 8
    private const val MIN_PIN = 4
    private const val MAX_PIN = 12

    /** Arma el PIN block en claro. */
    fun encode(pin: String, pan: String): ByteArray {
        require(pin.length in MIN_PIN..MAX_PIN) {
            "El PIN debe tener entre $MIN_PIN y $MAX_PIN dígitos, tiene ${pin.length}"
        }
        require(pin.all { it.isDigit() }) { "El PIN debe ser solo dígitos" }
        val pinField = ("0" + pin.length.toString(16).uppercase() + pin).padEnd(16, 'F')
        return pinField.hexToBytes() xorWith panField(pan)
    }

    /** Recupera el PIN desde un PIN block en claro. */
    fun decode(block: ByteArray, pan: String): String {
        require(block.size == BLOCK_SIZE) {
            "Un PIN block tiene $BLOCK_SIZE bytes, llegaron ${block.size}"
        }
        val pinField = (block xorWith panField(pan)).toHex()
        require(pinField[0] == '0') { "No es un PIN block formato 0" }
        val length = Character.digit(pinField[1], 16)
        require(length in MIN_PIN..MAX_PIN) { "Largo de PIN inválido: $length" }
        val pin = pinField.substring(2, 2 + length)
        require(pin.all { it.isDigit() }) { "El PIN block no descifra a dígitos: PAN equivocado" }
        return pin
    }

    /** Cifra el PIN con la variante de PIN de la llave de la transacción. */
    fun encrypt(transactionKey: ByteArray, pin: String, pan: String): ByteArray =
        Tdes.encrypt(Dukpt.pinKey(transactionKey), encode(pin, pan))

    /** Descifra un PIN block, que es lo que hace el HSM del host. */
    fun decrypt(transactionKey: ByteArray, encrypted: ByteArray, pan: String): String =
        decode(Tdes.decrypt(Dukpt.pinKey(transactionKey), encrypted), pan)

    /**
     * Campo de PAN: cuatro ceros y los 12 dígitos de la derecha del PAN sin el
     * dígito verificador.
     */
    private fun panField(pan: String): ByteArray {
        require(pan.length in 13..19) { "El PAN debe tener entre 13 y 19 dígitos, tiene ${pan.length}" }
        require(pan.all { it.isDigit() }) { "El PAN debe ser solo dígitos" }
        val withoutCheckDigit = pan.dropLast(1)
        return ("0000" + withoutCheckDigit.takeLast(12).padStart(12, '0')).hexToBytes()
    }
}
