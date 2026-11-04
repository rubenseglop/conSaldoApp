package com.example.saldoandalucia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saldoandalucia.data.ReadingRecord
import com.example.saldoandalucia.data.SavedCardRecord
import com.example.saldoandalucia.nfc.MifareCardReader
import com.example.saldoandalucia.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ScanState {
    object Idle : ScanState()
    object Reading : ScanState()
    data class Done(val result: MifareCardReader.ReadResult, val timestamp: Long) : ScanState()
}

private enum class Page { HOME, HISTORY, STATS, CARDS }

@Composable
fun CardScreen(
    state: ScanState,
    nfcAvailable: Boolean,
    cards: List<SavedCardRecord>,
    readings: List<ReadingRecord>,
    selectedUid: String?,
    onSelectCard: (String) -> Unit,
    onSaveNewCard: (String, String, Double, Long) -> Unit,
    onRenameCard: (String, String) -> Unit,
    onDeleteCard: (String) -> Unit,
    onSetThreshold: (String, Double) -> Unit
) {
    var page by remember { mutableStateOf(Page.HOME) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newCardName by remember { mutableStateOf("Mi tarjeta") }
    var editingCard by remember { mutableStateOf<SavedCardRecord?>(null) }
    var editName by remember { mutableStateOf("") }
    var thresholdCard by remember { mutableStateOf<SavedCardRecord?>(null) }
    var thresholdText by remember { mutableStateOf("5") }

    val successful = (state as? ScanState.Done)?.result as? MifareCardReader.ReadResult.Success
    val scanned = successful?.data
    val isNewCard = scanned != null && cards.none { it.uid == scanned.uid }

    LaunchedEffect(scanned?.uid, scanned?.balanceEuros, cards.size) {
        if (isNewCard) {
            newCardName = "Mi tarjeta"
            showSaveDialog = true
        } else if (scanned != null) {
            showSaveDialog = false
        }
    }

    SaldoAppTheme {
        Scaffold(
            containerColor = GreenDark,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF142B1B), contentColor = Color.White) {
                    NavigationBarItem(selected = page == Page.HOME, onClick = { page = Page.HOME }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") }, colors = navColors())
                    NavigationBarItem(selected = page == Page.HISTORY, onClick = { page = Page.HISTORY }, icon = { Icon(Icons.Default.History, null) }, label = { Text("Historial") }, colors = navColors())
                    NavigationBarItem(selected = page == Page.STATS, onClick = { page = Page.STATS }, icon = { Icon(Icons.Default.QueryStats, null) }, label = { Text("Estadísticas") }, colors = navColors())
                    NavigationBarItem(selected = page == Page.CARDS, onClick = { page = Page.CARDS }, icon = { Icon(Icons.Default.CreditCard, null) }, label = { Text("Tarjetas") }, colors = navColors())
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GreenDark, Color(0xFF0B1710)))).padding(padding).padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("💳 conSaldo", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = GreenLight)
                }
                Spacer(Modifier.height(18.dp))
                when (page) {
                    Page.HOME -> HomePage(state, nfcAvailable, cards, selectedUid, onSelectCard, onSetThreshold = { card -> thresholdCard = card; thresholdText = card.threshold.toInt().toString() })
                    Page.HISTORY -> HistoryPage(readings, cards)
                    Page.STATS -> StatsPage(cards, readings, selectedUid)
                    Page.CARDS -> CardsPage(cards, selectedUid, onSelectCard, onEdit = { editingCard = it; editName = it.name }, onDelete = onDeleteCard, onThreshold = { thresholdCard = it; thresholdText = it.threshold.toInt().toString() })
                }
            }
        }

        if (showSaveDialog && scanned != null) {
            AlertDialog(
                onDismissRequest = { /* La lectura no se guarda hasta confirmar */ },
                title = { Text("Guardar nueva tarjeta") },
                text = {
                    Column {
                        Text("Hemos leído una tarjeta que todavía no está guardada. ¿Qué nombre quieres asignarle?")
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = newCardName, onValueChange = { newCardName = it }, label = { Text("Nombre de la tarjeta") }, singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        Text("Saldo leído: ${money(scanned.balanceEuros)}", fontWeight = FontWeight.SemiBold)
                    }
                },
                confirmButton = { TextButton(onClick = { val name = newCardName.trim().ifBlank { "Mi tarjeta" }; onSaveNewCard(scanned.uid, name, scanned.balanceEuros, (state as ScanState.Done).timestamp); showSaveDialog = false }) { Text("Guardar") } },
                dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Ahora no") } }
            )
        }

        editingCard?.let { card ->
            AlertDialog(onDismissRequest = { editingCard = null }, title = { Text("Renombrar tarjeta") },
                text = { OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Nombre") }, singleLine = true) },
                confirmButton = { TextButton(onClick = { onRenameCard(card.uid, editName.trim().ifBlank { card.name }); editingCard = null }) { Text("Guardar") } },
                dismissButton = { TextButton(onClick = { editingCard = null }) { Text("Cancelar") } })
        }
        thresholdCard?.let { card ->
            AlertDialog(onDismissRequest = { thresholdCard = null }, title = { Text("Aviso de saldo bajo") },
                text = { Column { Text("Avisarme cuando el saldo sea inferior a:"); OutlinedTextField(value = thresholdText, onValueChange = { thresholdText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } }, label = { Text("Importe en euros") }, singleLine = true) } },
                confirmButton = { TextButton(onClick = { onSetThreshold(card.uid, thresholdText.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: 5.0); thresholdCard = null }) { Text("Guardar") } },
                dismissButton = { TextButton(onClick = { thresholdCard = null }) { Text("Cancelar") } })
        }
    }
}

@Composable private fun navColors() = NavigationBarItemDefaults.colors(selectedIconColor = GreenLight, selectedTextColor = GreenLight, indicatorColor = Color(0xFF244B32), unselectedIconColor = Color.LightGray, unselectedTextColor = Color.LightGray)

@Composable private fun HomePage(state: ScanState, nfcAvailable: Boolean, cards: List<SavedCardRecord>, selectedUid: String?, onSelectCard: (String) -> Unit, onSetThreshold: (SavedCardRecord) -> Unit) {
    val selected = cards.firstOrNull { it.uid == selectedUid } ?: cards.firstOrNull()
    if (cards.size > 1) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.name ?: "Seleccionar tarjeta", color = Color.White); Spacer(Modifier.weight(1f)); Text("⌄", color = Color.White) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { cards.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { onSelectCard(it.uid); expanded = false }) } }
        }
        Spacer(Modifier.height(12.dp))
    }
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CreditCard, null, tint = GreenLight, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(8.dp))
            val result = (state as? ScanState.Done)?.result as? MifareCardReader.ReadResult.Success
            val shownBalance = result?.data?.balanceEuros ?: selected?.balance
            Text(shownBalance?.let { money(it) } ?: "— €", color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black)
            Text(result?.let { "Saldo recién leído" } ?: selected?.let { "${it.name} · último saldo guardado" } ?: "Todavía no hay tarjetas guardadas", color = Color.LightGray, fontSize = 13.sp)
            if (selected != null) {
                Spacer(Modifier.height(12.dp))
                Text("Última consulta: ${dateTime(selected.lastRead)}", color = Color.LightGray, fontSize = 12.sp)
                TextButton(onClick = { onSetThreshold(selected) }) { Icon(Icons.Default.NotificationsActive, null); Spacer(Modifier.width(6.dp)); Text("Aviso si baja de ${money(selected.threshold)}") }
            }
            Spacer(Modifier.height(8.dp))
            when (state) {
                ScanState.Reading -> CircularProgressIndicator(color = GreenLight)
                is ScanState.Done -> when (val r = state.result) {
                    is MifareCardReader.ReadResult.Error -> Text(r.message, color = ErrorRed, textAlign = TextAlign.Center)
                    is MifareCardReader.ReadResult.Success -> Text("Tarjeta leída correctamente", color = GreenLight)
                }
                else -> Text(if (nfcAvailable) "Acerca tu tarjeta a la parte trasera del móvil" else "Este dispositivo no tiene NFC", color = Color.White, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Nfc, null, tint = GreenLight); Spacer(Modifier.width(8.dp)); Text("La lectura se inicia al acercar la tarjeta", color = Color.LightGray, fontSize = 12.sp) }
        }
    }
    if (cards.isEmpty()) {
        Spacer(Modifier.height(18.dp)); Text("Cuando escanees una tarjeta nueva, podrás guardarla con el nombre que quieras.", color = Color.LightGray, textAlign = TextAlign.Center)
    }
}

@Composable private fun HistoryPage(readings: List<ReadingRecord>, cards: List<SavedCardRecord>) {
    Text("Historial de consultas", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    if (readings.isEmpty()) EmptyMessage("Todavía no hay consultas guardadas.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(readings, key = { it.id }) { reading ->
            val name = cards.firstOrNull { it.uid == reading.uid }?.name ?: "Tarjeta"
            Surface(color = SurfaceCard, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(name, color = Color.White, fontWeight = FontWeight.SemiBold); Text(dateTime(reading.timestamp), color = Color.LightGray, fontSize = 12.sp) }
                    Text(money(reading.balance), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable private fun StatsPage(cards: List<SavedCardRecord>, readings: List<ReadingRecord>, selectedUid: String?) {
    val uid = selectedUid ?: cards.firstOrNull()?.uid
    val card = cards.firstOrNull { it.uid == uid }
    val now = System.currentTimeMillis()
    val monthStart = java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
    val list = readings.filter { it.uid == uid && it.timestamp >= monthStart }.sortedBy { it.timestamp }
    var consumption = 0.0
    for (i in 1 until list.size) if (list[i - 1].balance > list[i].balance) consumption += list[i - 1].balance - list[i].balance
    val increases = (1 until list.size).sumOf { i -> (list[i].balance - list[i - 1].balance).coerceAtLeast(0.0) }
    Text("Estadísticas", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    if (card == null) EmptyMessage("Guarda una tarjeta para consultar sus estadísticas.") else {
        StatTile("Consumo estimado este mes", money(consumption), "Suma de bajadas de saldo entre lecturas")
        Spacer(Modifier.height(10.dp)); StatTile("Recargas o aumentos detectados", money(increases), "No se puede confirmar el origen del aumento")
        Spacer(Modifier.height(10.dp)); StatTile("Consultas este mes", list.size.toString(), "Lecturas guardadas en este dispositivo")
        Spacer(Modifier.height(10.dp)); StatTile("Saldo más reciente", money(card.balance), "${card.name} · ${dateTime(card.lastRead)}")
    }
}

@Composable private fun CardsPage(cards: List<SavedCardRecord>, selectedUid: String?, onSelect: (String) -> Unit, onEdit: (SavedCardRecord) -> Unit, onDelete: (String) -> Unit, onThreshold: (SavedCardRecord) -> Unit) {
    Text("Mis tarjetas", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp)); Text("Escanea una tarjeta nueva para añadirla. Te preguntaremos antes de guardarla.", color = Color.LightGray, fontSize = 13.sp)
    Spacer(Modifier.height(12.dp))
    if (cards.isEmpty()) EmptyMessage("Aún no has guardado ninguna tarjeta.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(cards, key = { it.uid }) { card ->
            Surface(color = SurfaceCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { onSelect(card.uid) }) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CreditCard, null, tint = GreenLight); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(card.name, color = Color.White, fontWeight = FontWeight.Bold); Text(money(card.balance), color = GreenLight, fontSize = 20.sp, fontWeight = FontWeight.Bold) }; if (card.uid == selectedUid) Text("Activa", color = GreenLight, fontSize = 12.sp) }
                    Text("Última lectura: ${dateTime(card.lastRead)}", color = Color.LightGray, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { TextButton(onClick = { onEdit(card) }) { Text("Renombrar") }; TextButton(onClick = { onThreshold(card) }) { Text("Avisos") }; TextButton(onClick = { onDelete(card.uid) }) { Text("Eliminar", color = ErrorRed) } }
                }
            }
        }
    }
}

@Composable private fun StatTile(title: String, value: String, detail: String) { Surface(color = SurfaceCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, color = Color.LightGray, fontSize = 13.sp); Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold); Text(detail, color = Color.Gray, fontSize = 11.sp) } } }
@Composable private fun EmptyMessage(text: String) { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { Text(text, color = Color.LightGray, textAlign = TextAlign.Center) } }
private fun money(value: Double) = String.format(Locale("es", "ES"), "%.2f €", value)
private fun dateTime(timestamp: Long) = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "ES")).format(Date(timestamp))
