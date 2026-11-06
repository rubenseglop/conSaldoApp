package com.example.consaldo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
        } else if (scanned != null) {
            showSaveDialog = false
        }
    }

    SaldoAppTheme {
        Scaffold(
            containerColor = Color(0xFF09120D),
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF101A14),
                    contentColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = page == Page.HOME,
                        onClick = { page = Page.HOME },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Inicio") },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = page == Page.HISTORY,
                        onClick = { page = Page.HISTORY },
                        icon = { Icon(Icons.Default.History, null) },
                        label = { Text("Historial") },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = page == Page.STATS,
                        onClick = { page = Page.STATS },
                        icon = { Icon(Icons.Default.QueryStats, null) },
                        label = { Text("Resumen") },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = page == Page.CARDS,
                        onClick = { page = Page.CARDS },
                        icon = { Icon(Icons.Default.CreditCard, null) },
                        label = { Text("Tarjetas") },
                        colors = navColors()
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF14271A), Color(0xFF09120D))
                        )
                    )
                    .padding(padding)
            ) {
                AppHeader(page, Modifier.padding(horizontal = 22.dp, vertical = 16.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    when (page) {
                        Page.HOME -> HomePage(state, nfcAvailable, cards, selectedUid, onSelectCard)
                        Page.HISTORY -> HistoryPage(readings, cards)
                        Page.STATS -> StatsPage(cards, readings, selectedUid)
                        Page.CARDS -> CardsPage(
                            cards = cards,
                            selectedUid = selectedUid,
                            onSelect = onSelectCard,
                            onEdit = { editingCard = it; editName = it.name },
                            onDelete = onDeleteCard
                        )
                    }
                }
            }
        }

        if (showSaveDialog && scanned != null) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Guardar tarjeta", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Es una tarjeta nueva. ¿Qué nombre quieres ponerle?")
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = newCardName,
                            onValueChange = { newCardName = it },
                            label = { Text("Nombre de la tarjeta") },
                            singleLine = true
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Saldo: ${money(scanned.balanceEuros)}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = newCardName.trim().ifBlank { "Mi tarjeta" }
                            onSaveNewCard(
                                scanned.uid,
                                name,
                                scanned.balanceEuros,
                                (state as ScanState.Done).timestamp
                            )
                            showSaveDialog = false
                        }
                    ) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) { Text("Ahora no") }
                }
            )
        }

        editingCard?.let { card ->
            AlertDialog(
                onDismissRequest = { editingCard = null },
                title = { Text("Cambiar nombre", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRenameCard(card.uid, editName.trim().ifBlank { card.name })
                            editingCard = null
                        }
                    ) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { editingCard = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = GreenLight,
    selectedTextColor = GreenLight,
    indicatorColor = Color(0xFF244B32),
    unselectedIconColor = Color(0xFF9BA99E),
    unselectedTextColor = Color(0xFF9BA99E)
)

@Composable
private fun AppHeader(page: Page, modifier: Modifier = Modifier) {
    val title = when (page) {
        Page.HOME -> "Inicio"
        Page.HISTORY -> "Historial"
        Page.STATS -> "Resumen"
        Page.CARDS -> "Mis tarjetas"
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF244B32), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CreditCard, null, tint = GreenLight)
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Image(
                painter = painterResource(R.drawable.consaldo_wordmark),
                contentDescription = "conSaldo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )
            Text(title, color = Color(0xFFB7C7BA), fontSize = 14.sp)
        }
    }
}

@Composable
private fun HomePage(
    state: ScanState,
    nfcAvailable: Boolean,
    cards: List<SavedCardRecord>,
    selectedUid: String?,
    onSelectCard: (String) -> Unit
) {
    val selected = cards.firstOrNull { it.uid == selectedUid } ?: cards.firstOrNull()
    val result = (state as? ScanState.Done)?.result as? MifareCardReader.ReadResult.Success
    val shownBalance = result?.data?.balanceEuros ?: selected?.balance
    val scannedCard = result?.data?.let { data -> cards.firstOrNull { it.uid == data.uid } }
    val displayedName = if (result != null) {
        scannedCard?.name ?: "Tarjeta nueva"
    } else {
        selected?.name ?: "Mi tarjeta"
    }
    val lastRead = when {
        result != null -> "Lectura actualizada"
        selected != null -> "Última consulta · ${dateTime(selected.lastRead)}"
        else -> "Acerca tu tarjeta para consultar el saldo"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text(
                "Tu tarjeta de transporte",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Consulta el saldo acercándola al móvil",
                color = Color(0xFFB7C7BA),
                fontSize = 15.sp
            )
        }

        if (cards.size > 1) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(selected?.name ?: "Elegir tarjeta", modifier = Modifier.weight(1f))
                    Text("⌄", fontSize = 19.sp, color = GreenLight)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    cards.forEach { card ->
                        DropdownMenuItem(
                            text = { Text(card.name) },
                            onClick = { onSelectCard(card.uid); expanded = false }
                        )
                    }
                }
            }
        }

        TransportCard(
            name = displayedName,
            balance = shownBalance?.let { money(it) } ?: "— €",
            lastRead = lastRead
        )

        NfcStatusCard(state, nfcAvailable)

        if (cards.isEmpty()) {
            Text(
                "Cuando leas una tarjeta, podrás guardarla y ponerle un nombre.",
                color = Color(0xFFB7C7BA),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TransportCard(
    name: String,
    balance: String,
    lastRead: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(if (compact) 20.dp else 26.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 166.dp else 230.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF176B3A)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (compact) 0.dp else 5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF237B45), Color(0xFF155532), Color(0xFF103D27))
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 58.dp, y = (-78).dp)
                    .size(190.dp)
                    .background(Color.White.copy(alpha = 0.055f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 45.dp, y = 72.dp)
                    .size(145.dp)
                    .background(Color.White.copy(alpha = 0.045f), CircleShape)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 16.dp else 21.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 34.dp else 40.dp)
                            .background(Color.White.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(if (compact) 19.dp else 23.dp)
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "CONSORCIO DE TRANSPORTE",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = if (compact) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            name,
                            color = Color.White,
                            fontSize = if (compact) 15.sp else 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Icon(
                        Icons.Default.Nfc,
                        contentDescription = "NFC",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(if (compact) 22.dp else 26.dp)
                    )
                }

                Spacer(Modifier.height(if (compact) 10.dp else 15.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChipGraphic(compact)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "SALDO DISPONIBLE",
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = if (compact) 9.sp else 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            balance,
                            color = Color.White,
                            fontSize = if (compact) 25.sp else 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ANDALUCÍA",
                            color = Color.White,
                            fontSize = if (compact) 11.sp else 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.4.sp
                        )
                        Text(
                            lastRead,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = if (compact) 9.sp else 11.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        "CT",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = if (compact) 19.sp else 23.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipGraphic(compact: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(width = if (compact) 36.dp else 42.dp, height = if (compact) 27.dp else 32.dp)
            .background(Color(0xFFD3CC91), shape)
            .border(1.dp, Color(0xFFAAA575), shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.52f)
                .fillMaxHeight(0.62f)
                .border(1.dp, Color(0xFF8F895F), RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun NfcStatusCard(state: ScanState, nfcAvailable: Boolean) {
    val (message, detail, tint) = when (state) {
        ScanState.Reading -> Triple("Leyendo tarjeta…", "Mantén la tarjeta cerca del móvil", GreenLight)
        is ScanState.Done -> when (val result = state.result) {
            is MifareCardReader.ReadResult.Error ->
                Triple("No se pudo leer", result.message, ErrorRed)
            is MifareCardReader.ReadResult.Success ->
                Triple("Tarjeta leída correctamente", "El saldo se ha actualizado", GreenLight)
        }
        else -> if (nfcAvailable) {
            Triple("Lista para consultar", "Acerca la tarjeta a la parte trasera del móvil", GreenLight)
        } else {
            Triple("NFC no disponible", "Este móvil no tiene lector NFC", Color(0xFFFFC46B))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(20.dp)),
        color = Color(0xFF14231A),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state == ScanState.Reading) {
                CircularProgressIndicator(
                    color = GreenLight,
                    modifier = Modifier.size(27.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    Icons.Default.Nfc,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(27.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(message, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Color(0xFFB7C7BA), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun HistoryPage(readings: List<ReadingRecord>, cards: List<SavedCardRecord>) {
    if (readings.isEmpty()) {
        EmptyMessage("Todavía no hay consultas guardadas.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            item {
                PageHeading("Tus consultas", "${readings.size} registros guardados")
            }
            items(readings, key = { it.id }) { reading ->
                val name = cards.firstOrNull { it.uid == reading.uid }?.name ?: "Tarjeta"
                Surface(
                    color = Color(0xFF14231A),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF244B32), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, null, tint = GreenLight)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                dateTime(reading.timestamp),
                                color = Color(0xFF9BA99E),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            money(reading.balance),
                            color = GreenLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPage(
    cards: List<SavedCardRecord>,
    readings: List<ReadingRecord>,
    selectedUid: String?
) {
    val uid = selectedUid ?: cards.firstOrNull()?.uid
    val card = cards.firstOrNull { it.uid == uid }
    val monthStart = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val list = readings
        .filter { it.uid == uid && it.timestamp >= monthStart }
        .sortedBy { it.timestamp }
    var consumption = 0.0
    for (i in 1 until list.size) {
        if (list[i - 1].balance > list[i].balance) {
            consumption += list[i - 1].balance - list[i].balance
        }
    }
    val increases = (1 until list.size).sumOf { i ->
        (list[i].balance - list[i - 1].balance).coerceAtLeast(0.0)
    }

    if (card == null) {
        EmptyMessage("Guarda una tarjeta para ver aquí el resumen.")
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PageHeading("Este mes", "Actividad de ${card.name}")
            StatTile("Gasto aproximado", money(consumption), "Según los cambios de saldo")
            StatTile("Aumentos de saldo", money(increases), "Incluye posibles recargas")
            StatTile("Consultas realizadas", list.size.toString(), "Durante este mes")
            StatTile("Último saldo", money(card.balance), "Saldo guardado")
        }
    }
}

@Composable
private fun CardsPage(
    cards: List<SavedCardRecord>,
    selectedUid: String?,
    onSelect: (String) -> Unit,
    onEdit: (SavedCardRecord) -> Unit,
    onDelete: (String) -> Unit
) {
    if (cards.isEmpty()) {
        EmptyMessage("Aún no has guardado ninguna tarjeta. Acerca una tarjeta al móvil para añadirla.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                PageHeading("Tus tarjetas", "Acerca una tarjeta nueva para añadirla")
            }
            items(cards, key = { it.uid }) { card ->
                Surface(
                    color = Color(0xFF14231A),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (card.uid == selectedUid) {
                                Modifier.border(1.dp, GreenLight.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                            } else {
                                Modifier
                            }
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        TransportCard(
                            name = card.name,
                            balance = money(card.balance),
                            lastRead = "Última consulta · ${dateTime(card.lastRead)}",
                            compact = true,
                            modifier = Modifier.clickable { onSelect(card.uid) }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (card.uid == selectedUid) "ACTIVA" else "Toca la tarjeta para activar",
                                color = if (card.uid == selectedUid) GreenLight else Color(0xFF9BA99E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onEdit(card) }) { Text("Renombrar") }
                            TextButton(onClick = { onDelete(card.uid) }) {
                                Text("Eliminar", color = ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color(0xFF9BA99E), fontSize = 14.sp)
    }
}

@Composable
private fun StatTile(title: String, value: String, detail: String) {
    Surface(
        color = Color(0xFF14231A),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(title, color = Color(0xFFB7C7BA), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = Color(0xFF8E9E92), fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF14231A),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = GreenLight,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(text, color = Color(0xFFD9E2DB), textAlign = TextAlign.Center)
            }
        }
    }
}

private fun money(value: Double) = String.format(Locale("es", "ES"), "%.2f €", value)

private fun dateTime(timestamp: Long) =
    SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("es", "ES")).format(Date(timestamp))
