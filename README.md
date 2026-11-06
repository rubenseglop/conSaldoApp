# conSaldo 1.8.2

Aplicación Android para consultar el saldo de tarjetas MIFARE Classic 1K del Consorcio de Transportes de Andalucía.

conSaldo está diseñada para ser sencilla y fácil de entender, también para personas mayores. La consulta se realiza acercando la tarjeta a la parte trasera del móvil compatible con NFC.

## Características

- Lectura NFC de tarjetas MIFARE Classic 1K compatibles.
- Varias tarjetas guardadas en el propio móvil.
- La aplicación pregunta antes de guardar una tarjeta nueva y permite asignarle un nombre.
- Consulta del saldo y fecha de la última lectura.
- Historial local de consultas por tarjeta.
- Resumen de variaciones de saldo: las bajadas son estimaciones de consumo y las subidas son aumentos detectados; no identifica viajes ni confirma recargas.
- Widget Android que muestra el último saldo guardado. El widget no lee tarjetas NFC por sí mismo.
- Interfaz con textos, cifras y controles grandes, alto contraste e instrucciones directas.
- Tarjetas con una nueva presentación visual inspirada en las tarjetas de transporte de Andalucía.
- Logotipo gráfico de conSaldo en la cabecera de todas las pestañas.
- Sin publicidad, cuentas de usuario, servicios en la nube ni notificaciones.
- Los datos de las tarjetas y el historial se guardan localmente en el dispositivo.

## Requisitos

- Android 8.0 (API 26) o superior.
- Un móvil con NFC.
- Una tarjeta compatible con MIFARE Classic 1K.

La compatibilidad depende del hardware NFC del teléfono y de la tarjeta utilizada.

## Identificadores Android

- Nombre de la aplicación: `conSaldo`
- Namespace Kotlin/Android: `com.example.consaldo`
- applicationId: `com.example.consaldo`
- Versión: `1.8.2` (versionCode `11`)

**Importante sobre las actualizaciones:** al cambiar el identificador de aplicación, Android considera conSaldo una aplicación distinta de la versión anterior. Android considera conSaldo una aplicación distinta. Para instalar esta versión puede ser necesario desinstalar la anterior; al hacerlo, se borrarán los datos locales de la aplicación antigua. Haz una copia de los datos que quieras conservar antes de desinstalarla.

## Compilación

El proyecto utiliza Gradle y Android Gradle Plugin. En GitHub Actions, el workflow `.github/workflows/android-build.yml` ejecuta `gradle assembleDebug` y publica el APK como artefacto descargable.

Para compilar localmente, con Android SDK y Gradle instalados:

```bash
gradle assembleDebug
```

El APK de depuración se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Datos y privacidad

Las tarjetas guardadas y el historial se almacenan localmente en el móvil. La aplicación utiliza el identificador interno de la tarjeta para reconocerla, pero no lo muestra en la interfaz. No se envían estos datos a un servidor.

## Limitaciones

- El saldo solo se actualiza cuando se acerca y lee la tarjeta con NFC.
- El móvil no puede conocer cambios de saldo mientras la tarjeta no se está leyendo.
- El historial comienza cuando se guardan lecturas en la aplicación; no reconstruye movimientos anteriores.
- Las estadísticas son estimaciones basadas en diferencias entre lecturas.

## Publicación de versiones

Las versiones se identifican mediante etiquetas Git, por ejemplo `v1.8.2`. El workflow actual compila al recibir cambios en la rama `main` o al ejecutarse manualmente desde GitHub Actions.
