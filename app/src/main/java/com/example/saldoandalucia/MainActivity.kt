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
import com.example.saldoandalucia.data.LocalStore
import com.example.saldoandalucia.data.ReadingRecord
import com.example.saldoandalucia.data.SavedCardRecord
import com.example.saldoandalucia.nfc.MifareCardReader
import com.example.saldoandalucia.ui.CardScreen
import com.example.saldoandalucia.ui.ScanState
import com.example.saldoandalucia.widget.SaldoWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var showBrandSplash by mutableStateOf(true)
    private var uiState by mutableStateOf<ScanState>(ScanState.Idle)
    private lateinit var store: LocalStore
    private var savedCards by mutableStateOf<List<SavedCardRecord>>(emptyList())
    private var savedReadings by mutableStateOf<List<ReadingRecord>>(emptyList())
    private var selectedUid by mutableStateOf<String?>(null)


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        store = LocalStore(this)
        selectedUid = getPreferences(Context.MODE_PRIVATE).getString("selected_uid", null)
        refreshData()

        setContent {
            if (showBrandSplash) BrandSplashScreen() else CardScreen(
                state = uiState,
                nfcAvailable = nfcAdapter != null,
                cards = savedCards,
                readings = savedReadings,
                selectedUid = selectedUid,
                onSelectCard = { selectCard(it) },
                onSaveNewCard = { uid, name, balance, timestamp -> saveNewCard(uid, name, balance, timestamp) },
                onRenameCard = { uid, name -> store.renameCard(uid, name); refreshData(); SaldoWidgetProvider.updateAll(this) },
                onDeleteCard = { uid -> store.deleteCard(uid); if (selectedUid == uid) selectedUid = store.cards().firstOrNull()?.uid; persistSelected(); refreshData(); SaldoWidgetProvider.updateAll(this) }
            )
        }

        lifecycleScope.launch { delay(1800); showBrandSplash = false }
        handleIntent(intent)
    }

    override fun onResume() { super.onResume(); enableForegroundDispatch() }
    override fun onPause() { super.onPause(); nfcAdapter?.disableForegroundDispatch(this) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }

    private fun refreshData() {
        savedCards = store.cards()
        savedReadings = store.readings(null, 300)
        if (selectedUid != null && savedCards.none { it.uid == selectedUid }) selectedUid = savedCards.firstOrNull()?.uid
        if (selectedUid == null) selectedUid = savedCards.firstOrNull()?.uid
        persistSelected()
    }

    private fun selectCard(uid: String) { selectedUid = uid; persistSelected() }
    private fun persistSelected() { getPreferences(Context.MODE_PRIVATE).edit().putString("selected_uid", selectedUid).apply() }

    private fun saveNewCard(uid: String, name: String, balance: Double, timestamp: Long) {
        store.saveNewCard(uid, name, balance, timestamp)
        selectedUid = uid
        persistSelected()
        refreshData()
        SaldoWidgetProvider.updateAll(this)
    }

    private fun enableForegroundDispatch() {
        val adapter = nfcAdapter ?: return
        val intent = Intent(this, javaClass).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        adapter.enableForegroundDispatch(this, pendingIntent, null, arrayOf(arrayOf("android.nfc.tech.MifareClassic")))
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_TECH_DISCOVERED) return
        val tag: Tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return

        vibrarCorto(); sonarCorto(); uiState = ScanState.Reading
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { MifareCardReader(tag).readCard() }
            delay(300)
            val timestamp = System.currentTimeMillis()
            if (result is MifareCardReader.ReadResult.Success) {
                val data = result.data
                val existing = store.card(data.uid)
                if (existing != null) {
                    store.recordReading(data.uid, data.balanceEuros, timestamp)
                    selectedUid = data.uid
                    refreshData()
                    SaldoWidgetProvider.updateAll(this@MainActivity)
                }
            }
            uiState = ScanState.Done(result, timestamp)
        }
    }

    private fun vibrarCorto() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)) else { @Suppress("DEPRECATION") vibrator.vibrate(80) }
            }
        } catch (_: Exception) { }
    }

    private fun sonarCorto() { try { val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 70); tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120) } catch (_: Exception) { } }
}

@androidx.compose.runtime.Composable
private fun BrandSplashScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFF176B36)), contentAlignment = Alignment.Center) {
        Image(painter = painterResource(id = R.drawable.splash_logo_transparent), contentDescription = "conSaldo - Transportes Consorcio de Andalucía", modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 420.dp).padding(8.dp), contentScale = ContentScale.Fit)
    }
}
