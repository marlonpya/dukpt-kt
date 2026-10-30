package io.github.marlonpya.dukpt

/**
 * Recorre el ciclo completo con datos de prueba: inyección de la IPEK, tres
 * transacciones del terminal y la rederivación del lado del host.
 *
 * Todas las llaves son de prueba, las del anexo del estándar. No hay ni debe
 * haber una BDK real en ningún repositorio.
 */
fun main() {
    val bdk = "0123456789ABCDEFFEDCBA9876543210".hexToBytes()
    val pan = "4012345678909"
    val pin = "1234"

    println("=== Inyección de llave, una sola vez, en el KIF ===")
    var ksn = Ksn.parse("FFFF9876543210E00000")
    val ipek = Dukpt.deriveIpek(bdk, ksn)
    println("BDK   ${bdk.toHex()}   (vive solo en el HSM del adquirente)")
    println("KSN   $ksn")
    println("IPEK  ${ipek.toHex()}   (el terminal la usa para sus llaves futuras y la borra)")

    println()
    println("=== Tres transacciones del terminal ===")
    repeat(3) {
        ksn = ksn.next()
        val key = Dukpt.deriveTransactionKey(ipek, ksn)
        val block = PinBlock.encrypt(key, pin, pan)
        println("KSN $ksn  contador ${ksn.counter}")
        println("   llave de transacción ${key.toHex()}")
        println("   variante de PIN      ${Dukpt.pinKey(key).toHex()}")
        println("   PIN block cifrado    ${block.toHex()}")
        println("   viaja al host:       KSN + PIN block, ninguna llave")

        val recovered = PinBlock.decrypt(Dukpt.transactionKey(bdk, ksn), block, pan)
        println("   el host rederiva y obtiene el PIN: $recovered")
    }

    println()
    println("=== Por qué el contador no se puede repetir ===")
    val a = Dukpt.transactionKey(bdk, Ksn.parse("FFFF9876543210E00001"))
    val b = Dukpt.transactionKey(bdk, Ksn.parse("FFFF9876543210E00002"))
    println("contador 1  ${a.toHex()}")
    println("contador 2  ${b.toHex()}")
    println("La derivación es de un solo sentido: tener una no permite obtener la otra.")
}
