# conSaldo 1.6

Aplicación Android para consultar el saldo de tarjetas MIFARE Classic 1K del Consorcio de Transportes de Andalucía.

## Novedades 1.6
- Base de datos SQLite local para tarjetas e historial de consultas.
- Las tarjetas nuevas no se guardan automáticamente: la aplicación pregunta y permite asignar un nombre.
- Varias tarjetas, selección, renombrado y eliminación.
- Historial de lecturas con fecha, hora y saldo.
- Estadísticas de consumo estimado a partir de las bajadas observadas entre lecturas. No identifica viajes individuales.
- Aviso configurable de saldo bajo, evaluado al leer la tarjeta.
- Widget Android con el último saldo guardado (no realiza lecturas NFC por sí mismo).
- UID y datos técnicos de los bloques no se muestran en la interfaz.
- Copias de seguridad Android desactivadas para mantener los datos locales en el dispositivo.

## Compilación
El workflow de GitHub Actions compila `assembleDebug` y publica el APK como artefacto.

- applicationId: `com.example.saldoandalucia` (se mantiene para actualizar instalaciones anteriores)
- versionName: `1.6`
- versionCode: `7`
- Firma debug fija incluida en `app/debug.keystore` para mantener compatibilidad con las APK anteriores de este proyecto.

## Notas
- El historial empieza a acumularse desde la primera lectura que se guarda en esta versión; no puede reconstruir movimientos anteriores.
- El consumo es una estimación basada en diferencias entre consultas. Las subidas se registran como aumentos/posibles recargas, no como recargas confirmadas.
- Las notificaciones requieren permiso en Android 13 o superior y se evalúan cuando se lee una tarjeta guardada.
- El widget muestra el saldo de la tarjeta leída/seleccionada más recientemente; necesita abrir la app para actualizarse tras una lectura NFC.
