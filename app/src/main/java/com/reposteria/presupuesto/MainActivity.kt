package com.reposteria.presupuesto

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    var showHistory by remember { mutableStateOf(false) }
                    var loaded by remember { mutableStateOf<Presupuesto?>(null) }
                    var editorKey by remember { mutableStateOf(0) }
                    BackHandler(enabled = showHistory) { showHistory = false }
                    Box(Modifier.fillMaxSize()) {
                        // El editor se queda debajo para no perder lo que se está capturando
                        key(editorKey) {
                            EditorScreen(
                                initial = loaded,
                                onOpenHistory = { showHistory = true },
                                onNew = { loaded = null; editorKey++ }
                            )
                        }
                        if (showHistory) {
                            HistorialScreen(
                                onBack = { showHistory = false },
                                onOpen = { loaded = it; editorKey++; showHistory = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Una fila de insumo; los campos son texto para poder editarlos libremente
class InsumoRow(nombre: String = "", precio: String = "", cantidad: String = "1") {
    var nombre by mutableStateOf(nombre)
    var precio by mutableStateOf(precio)
    var cantidad by mutableStateOf(cantidad)
}

@Composable
fun NumField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    keyboard: KeyboardType = KeyboardType.Decimal
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard)
    )
}

@Composable
fun InsumoCard(row: InsumoRow, onRemove: () -> Unit) {
    val sub = (num(row.precio) ?: 0.0) * (num(row.cantidad) ?: 0.0)
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = row.nombre,
                onValueChange = { row.nombre = it },
                modifier = Modifier.weight(1f),
                label = { Text("Insumo") },
                singleLine = true
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Quitar", tint = Color(0xFFC62828))
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumField(row.precio, { row.precio = it }, "Precio", Modifier.weight(1f))
            NumField(row.cantidad, { row.cantidad = it }, "Cant. usada", Modifier.weight(1f))
            Text(
                "= " + money(sub),
                Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EditorScreen(initial: Presupuesto?, onOpenHistory: () -> Unit, onNew: () -> Unit) {
    val ctx = LocalContext.current
    var currentId by remember { mutableStateOf(initial?.id) }
    var nombre by remember { mutableStateOf(initial?.nombre ?: "") }
    val rows = remember {
        mutableStateListOf<InsumoRow>().apply {
            if (initial != null && initial.insumos.isNotEmpty()) {
                initial.insumos.forEach { add(InsumoRow(it.nombre, numText(it.precio), numText(it.cantidad))) }
            } else {
                add(InsumoRow())
            }
        }
    }
    var piezas by remember { mutableStateOf(if (initial != null && initial.piezas > 0) initial.piezas.toString() else "") }
    var venta by remember { mutableStateOf(if (initial != null && initial.precioVenta > 0) numText(initial.precioVenta) else "") }
    var margen by remember { mutableStateOf("50") }
    var askNew by remember { mutableStateOf(false) }

    fun draft(id: Long) = Presupuesto(
        id = id,
        fecha = nowText(),
        nombre = nombre.trim().ifEmpty { "Presupuesto" },
        insumos = rows
            .filter { it.nombre.isNotBlank() || (num(it.precio) ?: 0.0) > 0 }
            .map { Insumo(it.nombre.trim().ifEmpty { "Insumo" }, num(it.precio) ?: 0.0, num(it.cantidad) ?: 0.0) },
        piezas = piezas.toIntOrNull() ?: 0,
        precioVenta = num(venta) ?: 0.0
    )

    val p = draft(currentId ?: 0L)
    val listo = p.piezas > 0 && p.precioVenta > 0
    val perdida = listo && p.ganancia < 0
    val gPct = if (p.costoTotal > 0) p.ganancia / p.costoTotal * 100 else 0.0
    val sugerido = ceil(p.costoPieza * (1 + (num(margen) ?: 0.0) / 100))

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ---------- Barra superior ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Presupuesto", Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { askNew = true }) { Text("Nuevo", fontSize = 16.sp) }
            IconButton(onClick = onOpenHistory) { Icon(Icons.Default.History, "Guardados") }
        }

        // ---------- Resultados (siempre visibles) ----------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier.weight(1f).background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)).padding(12.dp)
            ) {
                Text("COSTO TOTAL", fontSize = 14.sp)
                Text(money(p.costoTotal), fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Text("${money(p.costoPieza)} por pieza", fontSize = 13.sp, maxLines = 1)
            }
            val bg = when {
                !listo -> Color(0xFFEEEEEE)
                perdida -> Color(0xFFFFCDD2)
                else -> Color(0xFFC8E6C9)
            }
            Column(Modifier.weight(1f).background(bg, RoundedCornerShape(12.dp)).padding(12.dp)) {
                Text(if (perdida) "PÉRDIDA" else "GANANCIA", fontSize = 14.sp)
                Text(
                    if (listo) money(abs(p.ganancia)) else "—",
                    fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                    color = if (perdida) Color(0xFFC62828) else Color(0xFF1B5E20)
                )
                Text(
                    if (listo) "${money(p.ganancia / p.piezas)} por pieza · " +
                        String.format(Locale.US, "%.0f", gPct) + "%"
                    else "pon piezas y precio",
                    fontSize = 13.sp, maxLines = 2
                )
            }
        }

        // ---------- Captura ----------
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre (ej. Pastel de chocolate)") },
                singleLine = true
            )

            Text("Insumos", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Ej.: harina a \$40 el kilo y usas 0.25 → \$10",
                fontSize = 13.sp, color = Color(0xFF555555)
            )
            rows.toList().forEach { row ->
                key(row) { InsumoCard(row, onRemove = { rows.remove(row) }) }
            }
            OutlinedButton(
                onClick = { rows.add(InsumoRow()) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Agregar insumo", fontSize = 16.sp)
            }

            Text("Venta", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(
                    piezas, { piezas = it.filter { c -> c.isDigit() } }, "Piezas que salen",
                    Modifier.weight(1f), KeyboardType.Number
                )
                NumField(venta, { venta = it }, "Precio de venta c/u", Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumField(margen, { margen = it }, "Ganancia deseada %", Modifier.weight(1f))
                Column(Modifier.weight(1f)) {
                    Text("Precio sugerido", fontSize = 13.sp)
                    Text(
                        money(sugerido) + " c/u",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1
                    )
                    TextButton(
                        onClick = { venta = numText(sugerido) },
                        enabled = sugerido > 0,
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Usar este precio") }
                }
            }
        }

        // ---------- Guardar / compartir ----------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    try {
                        val saved = draft(currentId ?: System.currentTimeMillis())
                        PresupuestoStore.save(ctx, saved)
                        currentId = saved.id
                        Toast.makeText(ctx, "Guardado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "No se pudo guardar", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar", fontSize = 18.sp) }
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, p.resumen())
                    }
                    ctx.startActivity(Intent.createChooser(send, "Compartir"))
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(6.dp))
                Text("Compartir", fontSize = 18.sp)
            }
        }
    }

    if (askNew) {
        AlertDialog(
            onDismissRequest = { askNew = false },
            title = { Text("¿Empezar uno nuevo?") },
            text = { Text("Lo que no hayas guardado se perderá.") },
            confirmButton = { TextButton(onClick = { askNew = false; onNew() }) { Text("Sí, nuevo") } },
            dismissButton = { TextButton(onClick = { askNew = false }) { Text("Cancelar") } }
        )
    }
}
