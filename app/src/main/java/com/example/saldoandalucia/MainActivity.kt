package com.example.saldoandalucia

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.saldoandalucia.nfc.MifareCardReader
import com.example.saldoandalucia.ui.CardScreen
import com.example.saldoandalucia.ui.ScanState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var showBrandSplash by mutableStateOf(true)

    // Estado que la UI de Compose observa: inactivo, leyendo, o resultado con hora
    private var uiState by mutableStateOf<ScanState>(ScanState.Idle)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // debe ir ANTES de super.onCreate
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            if (showBrandSplash) {
                BrandSplashScreen()
            } else {
                CardScreen(
                    state = uiState,
                    nfcAvailable = nfcAdapter != null
                )
            }
        }

        // Pantalla de bienvenida propia: imagen completa, sin el recorte
        // circular/cuadrado que Android aplica al icono del splash nativo.
        lifecycleScope.launch {
            delay(1800)
            showBrandSplash = false
        }

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

        // Feedback inmediato: asi sabes al instante que ha detectado un nuevo toque,
        // aunque el saldo que salga luego sea igual que la ultima vez.
        vibrarCorto()
        sonarCorto()
        uiState = ScanState.Reading

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                MifareCardReader(tag).readCard()
            }
            // Pequena espera minima para que "Leyendo..." se vea, aunque la lectura
            // real sea casi instantanea.
            delay(350)
            uiState = ScanState.Done(result, System.currentTimeMillis())
        }
    }

    private fun vibrarCorto() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            }
        } catch (_: Exception) {
            // Si el dispositivo no tiene vibrador, simplemente lo ignoramos.
        }
    }

    private fun sonarCorto() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {
            // Si falla (p.ej. en silencio total), no pasa nada.
        }
    }
}


@androidx.compose.runtime.Composable
private fun BrandSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF176B36)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_logo_transparent),
            contentDescription = "conSaldo - Transportes Consorcio de Andalucía",
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 420.dp)
                .padding(8.dp),
            contentScale = ContentScale.Fit
        )
    }
}
