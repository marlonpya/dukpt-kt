package io.github.marlonpya.dukpt

/**
 * Key Serial Number de 10 bytes: identificador del dispositivo más un contador
 * de transacciones de 21 bits.
 *
 * El KSN viaja en claro con cada mensaje y es lo único que el host necesita,
 * junto con la BDK, para rederivar la misma llave que usó el terminal. Nunca
 * viaja una llave.
 */
class Ksn private constructor(private val value: ByteArray) {

    /** Copia defensiva de los 10 bytes. */
    val bytes: ByteArray get() = value.copyOf()

    /** Contador de transacciones: los 21 bits de la derecha del KSN. */
    val counter: Int
        get() = ((value[7].toInt() and 0x1F) shl 16) or
            ((value[8].toInt() and 0xFF) shl 8) or
            (value[9].toInt() and 0xFF)

    /**
     * Los 8 bytes de la izquierda con el contador en cero.
     * Es la entrada de la derivación de la IPEK (X9.24-1, A.3).
     */
    internal val initialKeyRegister: ByteArray
        get() = value.copyOf(8).also { it[7] = (it[7].toInt() and 0xE0).toByte() }

    /**
     * Los 8 bytes de la derecha con el contador en cero.
     * Es el registro sobre el que se aplica el contador bit a bit (X9.24-1, A.2).
     */
    internal val derivationRegister: ByteArray
        get() = value.copyOfRange(2, 10).also {
            it[5] = (it[5].toInt() and 0xE0).toByte()
            it[6] = 0
            it[7] = 0
        }

    /** El mismo KSN con otro contador. */
    fun withCounter(newCounter: Int): Ksn {
        require(newCounter in 0..COUNTER_MASK) {
            "El contador debe estar entre 0 y $COUNTER_MASK, llegó $newCounter"
        }
        val b = value.copyOf()
        b[7] = ((b[7].toInt() and 0xE0) or ((newCounter ushr 16) and 0x1F)).toByte()
        b[8] = ((newCounter ushr 8) and 0xFF).toByte()
        b[9] = (newCounter and 0xFF).toByte()
        return Ksn(b)
    }

    /**
     * El KSN que el dispositivo usa en su siguiente transacción.
     *
     * El contador solo toma valores con [MAX_ONE_BITS] bits en uno o menos: esa
     * es la razón por la que un terminal agota su IPEK alrededor del millón de
     * transacciones y necesita una nueva inyección de llave.
     */
    fun next(): Ksn {
        var n = counter + 1
        while (n <= COUNTER_MASK && Integer.bitCount(n) > MAX_ONE_BITS) {
            n += n and -n
        }
        if (n > COUNTER_MASK) {
            throw KsnExhaustedException("El KSN $this agotó su contador: hace falta una nueva IPEK")
        }
        return withCounter(n)
    }

    fun toHex(): String = value.toHex()

    override fun toString(): String = toHex()

    override fun equals(other: Any?): Boolean = other is Ksn && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    companion object {
        /** Largo de un KSN, en bytes. */
        const val LENGTH = 10

        /** Máscara del contador de 21 bits. */
        const val COUNTER_MASK = 0x1FFFFF

        /** Un contador válido tiene como máximo estos bits en uno. */
        const val MAX_ONE_BITS = 10

        fun of(bytes: ByteArray): Ksn {
            require(bytes.size == LENGTH) {
                "Un KSN debe tener $LENGTH bytes, llegaron ${bytes.size}"
            }
            return Ksn(bytes.copyOf())
        }

        fun parse(hex: String): Ksn = of(hex.hexToBytes())
    }
}

/** El contador del KSN llegó a su límite y el terminal debe dejar de operar. */
class KsnExhaustedException(message: String) : IllegalStateException(message)
