# dukpt-kt

DUKPT con TDES (ANSI X9.24-1): IPEK, llave por transacción, variantes de PIN y MAC de solicitud, PIN block ISO 9564 formato 0.

Es parte del laboratorio de adquirencia: cinco repos hermanos que se clonan en la misma carpeta (ver `LEEME-PRIMERO.md` en la carpeta padre).

## Específico de este repo

- `./gradlew test` corre los vectores; `./gradlew run`, la demo.
- Pendiente a propósito: variantes de MAC de respuesta y de cifrado de datos (no hay fuente contrastada), AES DUKPT (X9.24-3), TR-31/TR-34.
- Cualquier cambio en la derivación debe seguir dando la IPEK del anexo A.4: `6AC292FAA1315B4D858AB3A3D7D5933A`.

## Reglas duras del laboratorio (aplican a todos los repos)

1. **Nunca loguear ni persistir PAN completo, pista 2, PIN ni PIN block.** Solo PAN enmascarado (6 primeros + 4 últimos). Los mensajes de error tampoco llevan el valor que falló.
2. **Ninguna llave real.** La única BDK permitida es la de prueba de ANSI X9.24-1 (`0123456789ABCDEFFEDCBA9876543210`). Nada de llaves en recursos ni en `BuildConfig`.
3. **Un timeout nunca es un rechazo.** Se reintenta la trama o la reversa, **nunca el cobro**. Un estado incierto se resuelve con reversa.
4. **No inventar valores de un estándar.** Si una máscara, un tag o un código no se puede contrastar con una fuente o un vector, se deja fuera y se anota en el README como pendiente.
5. **Cada operación criptográfica usa su propia llave DUKPT.** Para firmar algo que no viene de una lectura de tarjeta (reversa, aviso), primero se avanza el KSN.
6. **El dominio no conoce el framework.** Reglas en Kotlin puro y probadas sin red, sin emulador y sin servidor; Ktor, Room y Compose quedan en los bordes.

## Convenciones

- Kotlin 2.2.10, JVM 17, Gradle 9.6 (wrapper incluido). Código, KDoc, tests y commits en español.
- Tests con nombres que se leen como una regla (`@DisplayName` en JUnit 5 o nombres con backticks en JUnit 4).
- Conventional Commits: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `build:`, `ci:`.
- Los repos hermanos se consumen por *composite build*: si `../<repo>` existe, `settings.gradle.kts` lo incluye y se compila desde el código. Para cambiar una librería y probarla en la app no hay que publicar nada.
- Antes de cerrar un cambio: `./gradlew build` (o `./gradlew testDebugUnitTest assembleDebug` en `android-pos`) en verde.
