package com.example.saldoandalucia.model

/**
 * Representa una tarjeta guardada localmente en la app, con su último
 * saldo conocido. Pensado para ampliarse con Room (base de datos local)
 * si quieres guardar varias tarjetas con nombre e historial.
 */
data class SavedCard(
    val uid: String,
    val displayName: String,
    val lastBalanceEuros: Double,
    val cardTypeLabel: String,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)
