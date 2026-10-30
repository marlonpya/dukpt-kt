package io.github.marlonpya.dukpt

private const val HEX_DIGITS = "0123456789ABCDEF"

/** Convierte una cadena hexadecimal en bytes. Los espacios en blanco se ignoran. */
fun String.hexToBytes(): ByteArray {
    val clean = filterNot { it.isWhitespace() }
    require(clean.length % 2 == 0) {
        "Una cadena hexadecimal debe tener un número par de dígitos, tiene ${clean.length}"
    }
    return ByteArray(clean.length / 2) { i ->
        val hi = Character.digit(clean[i * 2], 16)
        val lo = Character.digit(clean[i * 2 + 1], 16)
        require(hi >= 0 && lo >= 0) { "Dígito hexadecimal inválido en la posición ${i * 2}" }
        ((hi shl 4) or lo).toByte()
    }
}

/** Representación hexadecimal en mayúsculas, sin separadores. */
fun ByteArray.toHex(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append(HEX_DIGITS[v ushr 4]).append(HEX_DIGITS[v and 0x0F])
    }
    return sb.toString()
}

/** XOR byte a byte. Si los arreglos difieren, se usa el largo del más corto. */
internal infix fun ByteArray.xorWith(other: ByteArray): ByteArray {
    val n = minOf(size, other.size)
    return ByteArray(n) { (this[it].toInt() xor other[it].toInt()).toByte() }
}
