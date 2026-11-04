package com.example.saldoandalucia.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saldoandalucia.nfc.MifareCardReader
import com.example.saldoandalucia.ui.theme.ErrorRed
import com.example.saldoandalucia.ui.theme.GreenDark
import com.example.saldoandalucia.ui.theme.GreenLight
import com.example.saldoandalucia.ui.theme.GreenPrimary
import com.example.saldoandalucia.ui.theme.SaldoAppTheme
import com.example.saldoandalucia.ui.theme.SurfaceCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Estado de la pantalla: sin leer nada todavia, leyendo en este momento,
 * o resultado ya obtenido (con la hora exacta en que se leyo, para que
 * el usuario siempre sepa que ha habido una lectura nueva).
 */
sealed class ScanState {
    object Idle : ScanState()
    object Reading : ScanState()
    data class Done(val result: MifareCardReader.ReadResult, val timestamp: Long) : ScanState()
}

@Composable
fun CardScreen(
    state: ScanState,
    nfcAvailable: Boolean
) {
    SaldoAppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(GreenDark, Color(0xFF0B0B0B)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💳 conSaldo",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(32.dp))

                when {
                    !nfcAvailable -> StatusMessage(
                        icon = Icons.Default.ErrorOutline,
                        text = "Este dispositivo no tiene NFC.",
                        tint = ErrorRed
                    )

                    state is ScanState.Idle -> ScanPrompt()

                    state is ScanState.Reading -> ReadingIndicator()

                    state is ScanState.Done -> {
                        when (val result = state.result) {
                            is MifareCardReader.ReadResult.Success ->
                                BalanceCard(result.data, state.timestamp)

                            is MifareCardReader.ReadResult.Error ->
                                StatusMessage(
                                    icon = Icons.Default.ErrorOutline,
                                    text = result.message,
                                    tint = ErrorRed
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanPrompt() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(GreenPrimary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = null,
                tint = GreenLight,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Acerca tu tarjeta verde\na la parte trasera del móvil",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

/**
 * Pantalla breve de "Leyendo..." con un icono pulsante, para que quede claro
 * que se ha detectado un nuevo toque de tarjeta, aunque el resultado final
 * sea igual que el anterior.
 */
@Composable
private fun ReadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(GreenPrimary.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Leyendo tarjeta…",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusMessage(icon: ImageVector, text: String, tint: Color) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp))
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = text, color = tint, textAlign = TextAlign.Center, fontSize = 16.sp)
}

@Composable
private fun BalanceCard(data: MifareCardReader.CardData, timestamp: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = GreenLight,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "%.2f €".format(data.balanceEuros),
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow(label = "Tipo", value = data.cardType.name)
            InfoRow(label = "UID", value = data.uid)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bloque de contrato (diagnóstico)",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Bloque 34 · Sector 8",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = data.contractBlockHex,
                color = GreenLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bloque 36 · Copia de contrato",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = data.backupContractBlockHex,
                color = GreenLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Leído a las ${formatTime(timestamp)}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
