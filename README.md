# Saldo Andalucía (proyecto base)

App Android (Kotlin + Jetpack Compose) para leer, por NFC, el saldo de las
tarjetas de transporte del Consorcio de Transporte Metropolitano de Andalucía
("tarjetas verdes"), tipo Mifare Classic 1K.

## ⚠️ Uso

Este proyecto es un punto de partida educativo. Úsalo solo con tarjetas de tu
propiedad. No lo emplees para alterar, clonar o manipular tarjetas ajenas, ni
para evadir pagos: eso sería ilegal independientemente de la intención con la
que se publique el código.

## Cómo abrirlo

1. Abre Android Studio → "Open" → selecciona esta carpeta.
2. Deja que Gradle sincronice (puede tardar la primera vez).
3. Conecta un móvil Android con NFC (min. Android 8.0) y ejecuta la app (▶).
4. Acerca la tarjeta a la parte trasera del teléfono. Aparecerá el saldo.

## Estructura

- `nfc/MifareCardReader.kt`: autenticación de sectores, lectura/escritura del
  "Value Block" de saldo, conversión a euros.
- `MainActivity.kt`: registra el *foreground dispatch* NFC y lanza la lectura
  cuando detecta un tag Mifare Classic.
- `ui/CardScreen.kt`: pantalla Compose que muestra saldo, tipo y UID.
- `model/SavedCard.kt`: modelo pensado para guardar varias tarjetas (puedes
  conectarlo a Room, que ya está incluido como dependencia).

## Puntos que tendrás que afinar tú mismo

- **Detección del tipo de tarjeta** (`detectCardType` en `MifareCardReader.kt`):
  el Consorcio no publica oficialmente el formato del bloque de contrato. El
  código trae un ejemplo con un byte de posición 0; tendrás que volcar varias
  tarjetas reales (Normal / Joven) y comparar los bytes para ajustar la
  heurística a lo que veas de verdad.
- **Escritura de saldo**: requiere la Key B del sector 9, que no se incluye
  (con razón: con ella se podría manipular el saldo). Solo funcionará si tú
  mismo conoces esa clave para tus propias tarjetas de pruebas.
- **Persistencia**: Room está añadido en las dependencias pero no hay
  Entity/DAO todavía — es el siguiente paso natural si quieres guardar
  histórico de varias tarjetas.

## Próximos pasos sugeridos

- Añadir una `Entity` + `Dao` de Room para guardar `SavedCard` con historial.
- Lista/carrusel de tarjetas guardadas en vez de pantalla única.
- Pantalla de ajustes para introducir la Key B manualmente por tarjeta.
