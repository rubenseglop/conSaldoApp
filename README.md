# conSaldo 1.4

Versión de diagnóstico NFC basada en conSaldo 1.3.

- Mantiene el applicationId y el keystore del proyecto para permitir actualizar la instalación anterior.
- Añade en el resultado del escaneo el volcado hexadecimal de los 16 bytes del bloque 34 (sector 8), utilizado provisionalmente para identificar el tipo de tarjeta.
- No cambia la lógica de identificación: sigue mostrando DESCONOCIDA cuando el formato no coincide con los códigos provisionales.

## Diagnóstico de tarjeta normal
Escanea una tarjeta normal del Consorcio y comparte el valor mostrado en “Bloque de contrato (diagnóstico)”. Ese dato permitirá determinar el criterio correcto sin adivinar códigos.
