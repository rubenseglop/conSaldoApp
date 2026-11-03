package com.example.saldoandalucia

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.saldoandalucia.nfc.MifareCardReader
import com.example.saldoandalucia.ui.CardScreen

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // Estado que la UI de Compose observa
    private var uiState by mutableStateOf<MifareCardReader.ReadResult?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            CardScreen(
                result = uiState,
                nfcAvailable = nfcAdapter != null
            )
        }

        // Si la app se abrió directamente al acercar la tarjeta
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Foreground dispatch: mientras la app está abierta, intercepta
     * cualquier tag NFC antes que el sistema, para poder leerlo al vuelo.
     */
    private fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val techLists = arrayOf(arrayOf("android.nfc.tech.MifareClassic"))
        adapter.enableForegroundDispatch(this, pendingIntent, null, techLists)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action != NfcAdapter.ACTION_TECH_DISCOVERED) return

        val tag: Tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return

        uiState = MifareCardReader(tag).readCard()
    }
}
