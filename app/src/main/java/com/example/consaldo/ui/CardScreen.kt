package com.example.consaldo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueryStats
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
import com.example.consaldo.data.ReadingRecord
import com.example.consaldo.data.SavedCardRecord
import com.example.consaldo.nfc.MifareCardReader
import com.example.consaldo.ui.theme.*
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
    onDeleteCard: (String) -> Unit
) {
    var page by remember { mutableStateOf(Page.HOME) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newCardName by remember { mutableStateOf("Mi tarjeta") }
    var editingCard by remember { mutableStateOf<SavedCardRecord?>(null) }
    var editName by remember { mutableStateOf("") }

    val successful = (state as? ScanState.Done)?.result as? MifareCardReader.ReadResult.Success
    val scanned = successful?.data
    val isNewCard = scanned != null && cards.none { it.uid == scanned.uid }

    LaunchedEffect(state, cards.size) {
        if (isNewCard) {
            newCardName = "Mi tarjeta"
            showSaveDialog = true
        } else if (scanned != null) showSaveDialog = false
    }

    SaldoAppTheme {
        Scaffold(
            containerColor = GreenDark,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF142B1B), contentColor = Color.White, tonalElevation = 0.dp) {
                    NavigationBarItem(selected = page == Page.HOME, onClick = { page = Page.HOME }, icon = { Icon(Icons.Default.Home, null, modifier = Modifier.size(27.dp)) }, label = { Text("Inicio", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }, colors = navColors())
                    NavigationBarItem(selected = page == Page.HISTORY, onClick = { page = Page.HISTORY }, icon = { Icon(Icons.Default.History, null, modifier = Modifier.size(27.dp)) }, label = { Text("Historial", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }, colors = navColors())
                    NavigationBarItem(selected = page == Page.STATS, onClick = { page = Page.STATS }, icon = { Icon(Icons.Default.QueryStats, null, modifier = Modifier.size(27.dp)) }, label = { Text("Resumen", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }, colors = navColors())
                    NavigationBarItem(selected = page == Page.CARDS, onClick = { page = Page.CARDS }, icon = { Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(27.dp)) }, label = { Text("Tarjetas", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }, colors = navColors())
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(GreenDark, Color(0xFF0B1710))))
                    .padding(padding).padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Text("conSaldo", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Consulta tu tarjeta de transporte", color = Color(0xFFE0F2E3), fontSize = 17.sp)
                Spacer(Modifier.height(22.dp))
                when (page) {
                    Page.HOME -> HomePage(state, nfcAvailable, cards, selectedUid, onSelectCard)
                    Page.HISTORY -> HistoryPage(readings, cards)
                    Page.STATS -> StatsPage(cards, readings, selectedUid)
                    Page.CARDS -> CardsPage(cards, selectedUid, onSelectCard,
                        onEdit = { editingCard = it; editName = it.name }, onDelete = onDeleteCard)
                }
            }
        }

        if (showSaveDialog && scanned != null) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Guardar tarjeta", fontSize = 23.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Es una tarjeta nueva. ¿Qué nombre quieres ponerle?", fontSize = 18.sp)
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(value = newCardName, onValueChange = { newCardName = it }, label = { Text("Nombre de la tarjeta") }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp))
                        Spacer(Modifier.height(10.dp))
                        Text("Saldo: ${money(scanned.balanceEuros)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = { TextButton(onClick = { val name = newCardName.trim().ifBlank { "Mi tarjeta" }; onSaveNewCard(scanned.uid, name, scanned.balanceEuros, (state as ScanState.Done).timestamp); showSaveDialog = false }) { Text("Guardar", fontSize = 18.sp) } },
                dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Ahora no", fontSize = 18.sp) } }
            )
        }
        editingCard?.let { card ->
            AlertDialog(onDismissRequest = { editingCard = null }, title = { Text("Cambiar nombre", fontSize = 23.sp) },
                text = { OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Nombre") }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp)) },
                confirmButton = { TextButton(onClick = { onRenameCard(card.uid, editName.trim().ifBlank { card.name }); editingCard = null }) { Text("Guardar", fontSize = 18.sp) } },
                dismissButton = { TextButton(onClick = { editingCard = null }) { Text("Cancelar", fontSize = 18.sp) } })
        }
    }
}

@Composable private fun navColors() = NavigationBarItemDefaults.colors(selectedIconColor = GreenLight, selectedTextColor = GreenLight, indicatorColor = Color(0xFF244B32), unselectedIconColor = Color.LightGray, unselectedTextColor = Color.LightGray)

@Composable private fun HomePage(state: ScanState, nfcAvailable: Boolean, cards: List<SavedCardRecord>, selectedUid: String?, onSelectCard: (String) -> Unit) {
    val selected = cards.firstOrNull { it.uid == selectedUid } ?: cards.firstOrNull()
    if (cards.size > 1) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
                Text(selected?.name ?: "Elegir tarjeta", color = Color.White, fontSize = 18.sp); Spacer(Modifier.weight(1f)); Text("▼", color = Color.White, fontSize = 16.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                cards.forEach { DropdownMenuItem(text = { Text(it.name, fontSize = 18.sp) }, onClick = { onSelectCard(it.uid); expanded = false }) }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CreditCard, null, tint = GreenLight, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(14.dp))
            Text("SALDO DISPONIBLE", color = Color(0xFFE0E0E0), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            val result = (state as? ScanState.Done)?.result as? MifareCardReader.ReadResult.Success
            val shownBalance = result?.data?.balanceEuros ?: selected?.balance
            Text(shownBalance?.let { money(it) } ?: "— €", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text(result?.let { "Saldo que acabas de consultar" } ?: selected?.let { "Último saldo de ${it.name}" } ?: "Todavía no has añadido ninguna tarjeta", color = Color(0xFFE0E0E0), fontSize = 17.sp, textAlign = TextAlign.Center)
            if (selected != null) {
                Spacer(Modifier.height(12.dp))
                Text("Última consulta: ${dateTime(selected.lastRead)}", color = Color.LightGray, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
            when (state) {
                ScanState.Reading -> { CircularProgressIndicator(color = GreenLight, modifier = Modifier.size(42.dp)); Spacer(Modifier.height(10.dp)); Text("Leyendo tarjeta…", color = Color.White, fontSize = 19.sp) }
                is ScanState.Done -> when (val r = state.result) {
                    is MifareCardReader.ReadResult.Error -> Text(r.message, color = ErrorRed, textAlign = TextAlign.Center, fontSize = 18.sp)
                    is MifareCardReader.ReadResult.Success -> Text("✓ Tarjeta leída correctamente", color = GreenLight, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                else -> {
                    Icon(Icons.Default.Nfc, null, tint = GreenLight, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(if (nfcAvailable) "Para consultar el saldo, acerca la tarjeta a la parte trasera del móvil" else "Este móvil no tiene lector NFC", color = Color.White, textAlign = TextAlign.Center, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    if (cards.isEmpty()) {
        Spacer(Modifier.height(20.dp))
        Text("Cuando acerques una tarjeta, podrás guardarla y ponerle un nombre.", color = Color.White, fontSize = 17.sp, textAlign = TextAlign.Center)
    }
}

@Composable private fun HistoryPage(readings: List<ReadingRecord>, cards: List<SavedCardRecord>) {
    Text("Consultas anteriores", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(14.dp))
    if (readings.isEmpty()) EmptyMessage("Todavía no hay consultas guardadas.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(readings, key = { it.id }) { reading ->
            val name = cards.firstOrNull { it.uid == reading.uid }?.name ?: "Tarjeta"
            Surface(color = SurfaceCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(dateTime(reading.timestamp), color = Color.LightGray, fontSize = 15.sp) }
                    Text(money(reading.balance), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                }
            }
        }
    }
}

@Composable private fun StatsPage(cards: List<SavedCardRecord>, readings: List<ReadingRecord>, selectedUid: String?) {
    val uid = selectedUid ?: cards.firstOrNull()?.uid
    val card = cards.firstOrNull { it.uid == uid }
    val monthStart = java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }.timeInMillis
    val list = readings.filter { it.uid == uid && it.timestamp >= monthStart }.sortedBy { it.timestamp }
    var consumption = 0.0
    for (i in 1 until list.size) if (list[i - 1].balance > list[i].balance) consumption += list[i - 1].balance - list[i].balance
    val increases = (1 until list.size).sumOf { i -> (list[i].balance - list[i - 1].balance).coerceAtLeast(0.0) }
    Text("Resumen de este mes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(14.dp))
    if (card == null) EmptyMessage("Guarda una tarjeta para ver aquí el resumen.") else {
        StatTile("Gasto aproximado", money(consumption), "Calculado comparando los saldos consultados")
        Spacer(Modifier.height(12.dp)); StatTile("Aumentos de saldo", money(increases), "Puede incluir recargas")
        Spacer(Modifier.height(12.dp)); StatTile("Veces que has consultado", list.size.toString(), "Durante este mes")
        Spacer(Modifier.height(12.dp)); StatTile("Último saldo", money(card.balance), "Tarjeta: ${card.name}")
    }
}

@Composable private fun CardsPage(cards: List<SavedCardRecord>, selectedUid: String?, onSelect: (String) -> Unit, onEdit: (SavedCardRecord) -> Unit, onDelete: (String) -> Unit) {
    Text("Mis tarjetas", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp)); Text("Acerca una tarjeta nueva al móvil para añadirla.", color = Color(0xFFE0E0E0), fontSize = 17.sp)
    Spacer(Modifier.height(14.dp))
    if (cards.isEmpty()) EmptyMessage("Aún no has guardado ninguna tarjeta.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(cards, key = { it.uid }) { card ->
            Surface(color = SurfaceCard, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onSelect(card.uid) }) {
                        Icon(Icons.Default.CreditCard, null, tint = GreenLight, modifier = Modifier.size(34.dp)); Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(card.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp); Text(money(card.balance), color = GreenLight, fontSize = 23.sp, fontWeight = FontWeight.Bold) }
                        if (card.uid == selectedUid) Text("Seleccionada", color = GreenLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Última consulta: ${dateTime(card.lastRead)}", color = Color.LightGray, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onEdit(card) }) { Text("Cambiar nombre", fontSize = 15.sp) }
                        TextButton(onClick = { onDelete(card.uid) }) { Text("Eliminar", color = ErrorRed, fontSize = 15.sp) }
                    }
                }
            }
        }
    }
}

@Composable private fun StatTile(title: String, value: String, detail: String) {
    Surface(color = SurfaceCard, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Color(0xFFE0E0E0), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = Color.LightGray, fontSize = 14.sp)
        }
    }
}
@Composable private fun EmptyMessage(text: String) { Box(Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) { Text(text, color = Color.White, textAlign = TextAlign.Center, fontSize = 18.sp) } }
private fun money(value: Double) = String.format(Locale("es", "ES"), "%.2f €", value)
private fun dateTime(timestamp: Long) = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "ES")).format(Date(timestamp))
