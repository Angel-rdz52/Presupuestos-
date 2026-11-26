package com.reposteria.presupuesto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun HistorialScreen(onBack: () -> Unit, onOpen: (Presupuesto) -> Unit) {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf(PresupuestoStore.loadAll(ctx).sortedByDescending { it.id }) }
    var toDelete by remember { mutableStateOf<Presupuesto?>(null) }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar") }
                Text("Presupuestos guardados", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            if (list.isEmpty()) {
                Text("Todavía no hay presupuestos guardados.", fontSize = 18.sp, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { p ->
                        val listo = p.piezas > 0 && p.precioVenta > 0
                        Row(
                            Modifier.fillMaxWidth()
                                .background(Color(0xFFF1F1F1), RoundedCornerShape(12.dp))
                                .clickable { onOpen(p) }
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(p.fecha, fontSize = 13.sp, color = Color(0xFF555555))
                                Text("Costo ${money(p.costoTotal)}", fontSize = 15.sp)
                                if (listo) {
                                    val perdida = p.ganancia < 0
                                    Text(
                                        (if (perdida) "Pérdida " else "Ganancia ") + money(abs(p.ganancia)) +
                                            " (vende a ${money(p.precioVenta)} c/u)",
                                        fontSize = 15.sp,
                                        color = if (perdida) Color(0xFFC62828) else Color(0xFF1B5E20)
                                    )
                                }
                            }
                            IconButton(onClick = { toDelete = p }) {
                                Icon(Icons.Default.Close, "Borrar", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar este presupuesto?") },
            text = { Text(p.nombre) },
            confirmButton = {
                TextButton(onClick = {
                    PresupuestoStore.delete(ctx, p.id)
                    list = list.filter { it.id != p.id }
                    toDelete = null
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } }
        )
    }
}
