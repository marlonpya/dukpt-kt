# dukpt-kt

Implementación en Kotlin de **DUKPT con TDES** (ANSI X9.24-1): derivación de la IPEK,
de la llave de cada transacción, sus variantes de PIN y MAC, y el PIN block
ISO 9564-1 formato 0.

Es un proyecto de estudio. Lo escribí para entender a nivel de código por qué
cada transacción usa una llave distinta, por qué lo único que viaja es el KSN y
por qué un terminal agota su llave inicial alrededor del millón de
transacciones. **No es código de producción y no reemplaza al hardware seguro:**
en un terminal real esta derivación ocurre dentro del procesador seguro y la
aplicación solo consume la API del SDK del fabricante, de modo que la llave
nunca está en la memoria de la app.

## Qué demuestra

| Archivo | Qué resuelve |
| --- | --- |
| `Ksn.kt` | Los 10 bytes del KSN, el contador de 21 bits y su avance, con la regla de los diez bits en uno que limita el ciclo de vida de la IPEK |
| `Dukpt.kt` | Derivación de la IPEK, proceso de generación de llave no reversible, llave de transacción y variantes de PIN y MAC |
| `PinBlock.kt` | PIN block formato 0: cómo el PAN entra en el bloque y por qué el mismo PIN da bloques distintos en dos tarjetas |
| `Tdes.kt` | DES y TDES en ECB sin relleno, que es lo que pide el estándar para bloques de 8 bytes |
| `Demo.kt` | El ciclo completo: inyección, tres transacciones y la rederivación del lado del host |

## Cómo correrlo

```bash
gradle wrapper        # una sola vez, para añadir el wrapper al repo
./gradlew test        # los vectores del anexo
./gradlew run         # la demostración del ciclo completo
```

## Cómo se validó

Dos comprobaciones independientes:

1. **Vectores del estándar.** La IPEK de ANSI X9.24-1 Annex A.4 para
   `BDK = 0123456789ABCDEFFEDCBA9876543210` y `KSN = FFFF9876543210E00000` es
   `6AC292FAA1315B4D858AB3A3D7D5933A`, y es la que produce esta implementación.
2. **Contraste contra otra implementación.** Las llaves de transacción, las
   variantes de PIN y MAC, los PIN blocks cifrados y 3.000 avances consecutivos
   del contador se compararon byte a byte contra una implementación
   independiente del mismo estándar, sin diferencias.

Para contrastar por tu cuenta, la implementación en C de
[openemv/dukpt](https://github.com/openemv/dukpt) cubre TDES y AES e incluye una
herramienta de línea de comandos que sirve de oráculo.

## Lo que no está implementado, a propósito

- **Las variantes de MAC de respuesta y de cifrado de datos.** Las fuentes que
  tuve a mano no coinciden en qué máscara corresponde a cada una, así que
  preferí dejarlas fuera antes que adivinar. Se agregan cuando se confirmen
  contra la Tabla A-1 del estándar.
- **AES DUKPT (X9.24-3).** Es el esquema vigente frente a TDES y es el siguiente
  paso natural del proyecto.
- **TR-31 y TR-34**, que son los formatos en los que las llaves se transportan e
  inyectan de verdad.
- **Resistencia a canales laterales.** Esta implementación corre sobre
  `javax.crypto` en una JVM normal; un terminal certificado PCI PTS resuelve eso
  en hardware.

## Seguridad

Todas las llaves, PAN y PIN de este repositorio son los valores de prueba
publicados en el anexo del estándar. No hay datos reales, y el pipeline incluye
un escaneo de secretos sobre el historial para que siga siendo así.

## Licencia

MIT.
