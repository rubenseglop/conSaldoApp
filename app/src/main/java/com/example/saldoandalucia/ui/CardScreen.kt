package com.example.saldoandalucia.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.saldoandalucia.nfc.MifareCardReader

@Composable
fun CardScreen(
    result: MifareCardReader.ReadResult?,
    nfcAvailable: Boolean
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Saldo Andalucía",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (!nfcAvailable) {
                    Text("Este dispositivo no tiene NFC.")
                    return@Column
                }

                when (result) {
                    null -> Text("Acerca tu tarjeta verde a la parte trasera del teléfono.")

                    is MifareCardReader.ReadResult.Success -> {
                        val data = result.data
                        Text(
                            text = "%.2f €".format(data.balanceEuros),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tipo: ${data.cardType}")
                        Text("UID: ${data.uid}")
                    }

                    is MifareCardReader.ReadResult.Error -> {
                        Text(
                            text = result.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
