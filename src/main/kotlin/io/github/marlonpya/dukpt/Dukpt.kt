package io.github.marlonpya.dukpt

/**
 * DUKPT con TDES, según ANSI X9.24-1.
 *
 * Las tres ideas que sostienen el esquema:
 *
 *  1. La BDK vive solo en el HSM del adquirente y nunca sale de ahí.
 *  2. El terminal recibe una sola vez su IPEK, derivada de la BDK y de su KSN
 *     inicial, en un proceso físico y auditado (el KIF).
 *  3. En cada transacción el terminal deriva una llave nueva a partir del
 *     contador. La derivación es de un solo sentido, así que comprometer una
 *     llave no expone las anteriores ni las siguientes, y lo único que viaja
 *     es el KSN.
 *
 * Esta implementación es para entender y validar el esquema. En un terminal
 * real la derivación ocurre dentro del hardware seguro y la aplicación solo
 * consume la API del SDK del fabricante: la llave nunca está en memoria de la
 * app.
 */
object Dukpt {

    /** Largo de la BDK, de la IPEK y de toda llave derivada. */
    const val KEY_LENGTH = 16

    /** Máscara usada en la mitad derecha de la IPEK y en cada derivación. */
    private val KEY_MASK = "C0C0C0C000000000C0C0C0C000000000".hexToBytes()

    /** Variante de la llave de transacción para cifrar el PIN. */
    private val PIN_VARIANT = "00000000000000FF00000000000000FF".hexToBytes()

    /** Variante de la llave de transacción para el MAC de los mensajes de solicitud. */
    private val MAC_REQUEST_VARIANT = "000000000000FF00000000000000FF00".hexToBytes()

    /**
     * Deriva la IPEK desde la BDK y el KSN inicial.
     *
     * Es lo que hace el HSM dentro del KIF, una sola vez por terminal. El
     * terminal genera con ella sus llaves futuras y borra la IPEK.
     */
    fun deriveIpek(bdk: ByteArray, ksn: Ksn): ByteArray {
        requireKey(bdk, "La BDK")
        val base = ksn.initialKeyRegister
        return Tdes.encrypt(bdk, base) + Tdes.encrypt(bdk xorWith KEY_MASK, base)
    }

    /**
     * Deriva la llave de la transacción identificada por el contador del [ksn].
     *
     * Recorre los 21 bits del contador de mayor a menor y aplica una derivación
     * por cada bit encendido, así que el costo es a lo más [Ksn.MAX_ONE_BITS]
     * pasos, no el valor del contador.
     */
    fun deriveTransactionKey(ipek: ByteArray, ksn: Ksn): ByteArray {
        requireKey(ipek, "La IPEK")
        val register = ksn.derivationRegister
        val counter = ksn.counter
        var key = ipek.copyOf()
        var shift = 0x100000
        while (shift > 0) {
            if (shift and counter != 0) {
                register[5] = ((register[5].toInt() and 0xFF) or (shift ushr 16)).toByte()
                register[6] = ((register[6].toInt() and 0xFF) or ((shift ushr 8) and 0xFF)).toByte()
                register[7] = ((register[7].toInt() and 0xFF) or (shift and 0xFF)).toByte()
                key = generateKey(key, register)
            }
            shift = shift ushr 1
        }
        return key
    }

    /**
     * Atajo del lado del host: desde la BDK y el KSN recibido, la llave de esa
     * transacción. Es exactamente lo que hace el HSM al recibir un mensaje.
     */
    fun transactionKey(bdk: ByteArray, ksn: Ksn): ByteArray =
        deriveTransactionKey(deriveIpek(bdk, ksn), ksn)

    /** Llave de cifrado de PIN para esta transacción. */
    fun pinKey(transactionKey: ByteArray): ByteArray = variant(transactionKey, PIN_VARIANT)

    /** Llave de MAC para los mensajes de solicitud de esta transacción. */
    fun macRequestKey(transactionKey: ByteArray): ByteArray =
        variant(transactionKey, MAC_REQUEST_VARIANT)

    private fun variant(key: ByteArray, mask: ByteArray): ByteArray {
        requireKey(key, "La llave de transacción")
        return key xorWith mask
    }

    /** Proceso de generación de llave no reversible (X9.24-1, A.2). */
    private fun generateKey(key: ByteArray, register: ByteArray): ByteArray =
        encryptRegister(key xorWith KEY_MASK, register) + encryptRegister(key, register)

    private fun encryptRegister(key: ByteArray, register: ByteArray): ByteArray {
        val left = key.copyOf(8)
        val right = key.copyOfRange(8, 16)
        return right xorWith Tdes.desEncrypt(left, right xorWith register)
    }

    private fun requireKey(key: ByteArray, name: String) {
        require(key.size == KEY_LENGTH) {
            "$name debe tener $KEY_LENGTH bytes, tiene ${key.size}"
        }
    }
}
